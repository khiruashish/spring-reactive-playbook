# WebTestClient

`WebTestClient` tests HTTP endpoints. It can bind to a controller, a router function, an
`ApplicationContext`, or a real running server.

| Binding | Starts | Use |
|---|---|---|
| `bindToController(...)` | nothing — controller instances you pass | fast unit-style tests |
| `bindToRouterFunction(...)` | nothing | functional endpoints |
| `bindToApplicationContext(ctx)` / `@WebFluxTest` + `@Autowired` | Spring context (web slice) | annotated + config + security |
| `bindToServer().baseUrl(...)` / `@SpringBootTest(webEnvironment = RANDOM_PORT)` | the real server | end-to-end over a socket |

## Controller binding

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/web/WebFluxExamplesTest.java:bindToController"
```

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/web/WebFluxExamplesTest.java:webTestClient"
```

## Router binding

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/web/WebFluxExamplesTest.java:routerTest"
```

## `@WebFluxTest` slice (Boot 4)

```java
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;   // Boot 4 package
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebFluxTest(ProductController.class)
class ProductControllerSliceTest {

    @Autowired WebTestClient client;
    @MockitoBean ProductRepository repository;        // replaces @MockBean

    @Test
    void returnsProduct() {
        when(repository.findById("1")).thenReturn(Mono.just(new Product("1", "Pen", "office", 199)));
        client.get().uri("/products/1").exchange()
                .expectStatus().isOk()
                .expectBody(Product.class).isEqualTo(new Product("1", "Pen", "office", 199));
    }
}
```

Test dependency: `testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")`.

## Streaming responses

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/web/WebFluxExamplesTest.java:sseTest"
```
