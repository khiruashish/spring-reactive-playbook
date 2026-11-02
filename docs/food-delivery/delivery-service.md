# Delivery Service

**Role:** assigns a courier when an order is paid and simulates the trip, publishing every move.
**Topics:** [Reactive MongoDB](../data/mongodb.md), [Kafka](../messaging/kafka.md),
[Resources & Cancellation](../reactor/resources-and-cancellation.md), [SSE](../webflux/streaming-sse.md).

## The trip is a pure, time-driven Flux

```java
--8<-- "food-delivery/delivery-service/src/main/java/com/example/fooddelivery/delivery/CourierSimulator.java:route"
```

## Running it: `concatMap` + a managed `Disposable`

```java
--8<-- "food-delivery/delivery-service/src/main/java/com/example/fooddelivery/delivery/DeliveryService.java:trip"
```

- `concatMap` guarantees each waypoint is saved and published **before** the next one starts.
- The trip outlives the Kafka message that started it, so it is a deliberate detached `subscribe()` —
  tracked in a `Disposable.Composite` and disposed on shutdown (`@PreDestroy`).

## Idempotent assignment

```java
return repository.save(delivery)
        ...
        .onErrorResume(DuplicateKeyException.class, dup -> repository.findByOrderId(order.orderId()));
```

`@Indexed(unique = true) orderId` turns a redelivered `OrderPlaced` into a no-op.

## Tracking stream

`GET /api/deliveries/{orderId}/track` (SSE) emits the last known position, then live updates, completing
on `DELIVERED`. If the delivery doesn't exist yet, `switchIfEmpty(live)` simply waits for it.

## Test: minutes of trip in milliseconds

```java
StepVerifier.withVirtualTime(() -> CourierSimulator.route(0, 0, 4, 8, 4, Duration.ofMinutes(1)))
        .expectSubscription()
        .expectNoEvent(Duration.ofMinutes(1))
        .assertNext(wp -> assertThat(wp.status()).isEqualTo(DeliveryStatus.PICKED_UP))
        .thenAwait(Duration.ofMinutes(3))
        .expectNextMatches(wp -> wp.status() == DeliveryStatus.IN_TRANSIT && wp.latitude() == 1.0)
        .expectNextCount(2)
        .thenAwait(Duration.ofMinutes(1))
        .expectNext(new Waypoint(DeliveryStatus.DELIVERED, 4, 8))
        .verifyComplete();
```
