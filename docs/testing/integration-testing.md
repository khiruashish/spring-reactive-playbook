# Integration Tests

Use real infrastructure in containers for anything involving SQL dialects, Mongo queries, Kafka
semantics or serialization across process boundaries.

```kotlin
testImplementation("org.springframework.boot:spring-boot-testcontainers")
testImplementation("org.testcontainers:testcontainers-postgresql")
testImplementation("org.testcontainers:testcontainers-kafka")
testImplementation("org.testcontainers:testcontainers-mongodb")
testImplementation("org.testcontainers:testcontainers-junit-jupiter")
```

## `@ServiceConnection` — zero-config wiring

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderFlowIT {

    @Container @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Container @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.1.0");

    @Autowired WebTestClient client;   // bound to the running server

    @Test
    void placesOrder() {
        client.post().uri("/api/orders").header("X-User-Id", "it-user")
                .bodyValue(Map.of("restaurantId", 1, "deliveryAddress", "x",
                                  "items", List.of(Map.of("menuItemId", 1, "quantity", 1))))
                .exchange()
                .expectStatus().isCreated();
    }
}
```

`@ServiceConnection` produces the matching `ConnectionDetails` beans, so Boot builds an
**R2DBC** `ConnectionFactory` (and Kafka/Mongo clients) pointing at the container — no
`@DynamicPropertySource` needed.

## Data slices

```java
@DataR2dbcTest          // org.springframework.boot.data.r2dbc.test.autoconfigure
@Testcontainers
class ProductRowRepositoryTest {
    @Container @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired ProductRowRepository repository;

    @Test
    void derivedQuery() {
        StepVerifier.create(repository.save(new ProductRow(null, "Pen", "office", 199, Instant.now()))
                        .thenMany(repository.findByCategoryOrderByName("office")))
                .expectNextMatches(p -> p.name().equals("Pen"))
                .verifyComplete();
    }
}
```

## Waiting for asynchronous outcomes

Kafka-driven effects happen *eventually*. Poll reactively instead of sleeping:

```java
StepVerifier.create(Mono.defer(() -> deliveries.findByOrderId(orderId))
                .repeatWhenEmpty(r -> r.delayElements(Duration.ofMillis(200)).take(50)))
        .expectNextMatches(d -> d.courierId() != null)
        .verifyComplete();
```

(Awaitility works too.)

## Contract tests

For service boundaries, add consumer-driven contracts (Pact, Spring Cloud Contract) so a provider
change that breaks a consumer fails the provider's build.
