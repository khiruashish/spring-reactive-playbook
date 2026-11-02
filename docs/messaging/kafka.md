# Kafka in Reactive Applications

```kotlin
implementation("org.springframework.boot:spring-boot-starter-kafka")
```

!!! info "What about reactor-kafka?"
    The `reactor-kafka` project (`KafkaReceiver`/`KafkaSender`) has been discontinued, and Spring Kafka
    deprecated its `ReactiveKafka*Template` wrappers. The approach below uses **standard Spring Kafka**
    with reactive return types — fully supported and simpler to operate.

## Producing without blocking

`KafkaTemplate.send(...)` returns a `CompletableFuture<SendResult>`. Adapt it lazily:

```java
@Component
class PriceEventsPublisher {
    private final KafkaTemplate<String, String> kafka;
    private final JsonMapper json;                                  // Jackson 3

    Mono<Void> publish(PriceChanged event) {
        return Mono.fromCallable(() -> json.writeValueAsString(event))
                .flatMap(payload -> Mono.fromFuture(() -> kafka.send("catalog.price.changed.v1",
                        event.productId(), payload)))               // key → partition → per-key ordering
                .then();
    }
}
```

- `Mono.fromFuture(() -> ...)` (supplier form) sends only on subscription.
- The **record key** decides the partition; all events with the same key stay ordered.
- Producer safety: `acks=all`, `enable.idempotence=true` (defaults in modern clients).

## Consuming asynchronously

Since Spring Kafka 3.2, a `@KafkaListener` may return `Mono<?>` or `CompletableFuture<?>`. The container
treats it as **async**: the listener thread is released, and the offset is committed when the `Mono`
completes (out-of-order commits are handled for you).

```java
@KafkaListener(topics = "catalog.price.changed.v1", groupId = "search-indexer")
public Mono<Void> onPriceChanged(String payload) {
    return Mono.fromCallable(() -> json.readValue(payload, PriceChanged.class))
            .flatMap(indexer::reindex)                  // non-blocking work
            .then();
}
```

Errors signalled by the `Mono` go through the container's error handler (retry, DLT via
`DeadLetterPublishingRecoverer`, `@RetryableTopic`).

## Delivery semantics checklist

| Concern | Practice |
|---|---|
| At-least-once delivery → duplicates | make consumers **idempotent** (unique keys, upserts, processed-event table) |
| DB write + publish atomically | **transactional outbox** (+ Debezium CDC or poller) |
| Ordering | same key → same partition; process one key sequentially (`concatMap`) |
| Poison messages | bounded retries, then a dead-letter topic |
| Schema evolution | versioned topics (`…v1`) or Schema Registry (Avro/Protobuf) |

## Kafka → browser

A very common reactive pattern: consume a topic, push into a `Sinks.Many`, expose it as SSE or
WebSocket. Each service instance should use its **own consumer group** (or unique group id) for this
broadcast, so every instance sees every event. The capstone's notification-service does exactly this.
