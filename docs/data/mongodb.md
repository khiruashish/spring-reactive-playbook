# Reactive MongoDB

```kotlin
implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
```

```yaml
spring:
  mongodb:
    uri: mongodb://localhost:27017/catalog     # Boot 4 property (was spring.data.mongodb.uri)
  data:
    mongodb:
      auto-index-creation: true
```

## Documents and repositories

```java
@Document("reviews")
public record Review(@Id String id, @Indexed String productId, int stars, String text, Instant at) { }

public interface ReviewRepository extends ReactiveMongoRepository<Review, String> {
    Flux<Review> findByProductIdOrderByAtDesc(String productId);
    Mono<Long> countByProductIdAndStarsGreaterThanEqual(String productId, int stars);
}
```

## `ReactiveMongoTemplate`

```java
Mono<UpdateResult> bump = template.updateFirst(
        query(where("productId").is(id)),
        new Update().inc("views", 1),
        ProductStats.class);

Flux<RatingSummary> summary = template.aggregate(newAggregation(
        match(where("productId").is(id)),
        group("stars").count().as("n")), "reviews", RatingSummary.class);
```

## Streams of changes

=== "Change streams (replica set required)"

    ```java
    Flux<Review> live = template.changeStream(Review.class)
            .watchCollection("reviews")
            .filter(where("operationType").is("insert"))
            .listen()
            .mapNotNull(ChangeStreamEvent::getBody);
    ```

=== "Tailable cursor (capped collection)"

    ```java
    public interface LogRepository extends ReactiveMongoRepository<LogLine, String> {
        @Tailable
        Flux<LogLine> findBySeverity(String severity);   // stays open, emits new documents
    }
    ```

These map perfectly onto SSE endpoints: `return reviewChanges.filter(...)` and the browser receives
inserts live.

## Transactions

Multi-document transactions need a replica set and `ReactiveMongoTransactionManager`; then
`@Transactional` / `TransactionalOperator` work as in [Reactive Transactions](transactions.md).
