# Order Service

**Role:** the heart of the system — prices carts, persists orders, takes payment, publishes events,
tracks status. **Topics:** almost all of them.

## Layout (ports & adapters)

```text
order/
├── domain/          Order, OrderLine, OrderStatus (state machine), Pricing (pure), Ports (interfaces)
├── application/     OrderService (use case pipeline), OrderStatusStream (Sinks)
├── infrastructure/  R2dbcOrderStore, HttpMenuCatalog, HttpPayments, KafkaOrderEvents, DeliveryStatusListener
├── api/             OrderController, DTOs, exception advice
└── config/          WebClient/HTTP interface beans, circuit breaker
```

## The place-order pipeline

```java
public Mono<Order> place(String idempotencyKey, PlaceOrder command) {
    return store.findByIdempotencyKey(idempotencyKey)                       // replay → original order
            .switchIfEmpty(Mono.defer(() -> createAndPay(idempotencyKey, command)));
}

private Mono<Order> createAndPay(String idempotencyKey, PlaceOrder command) {
    return catalog.quotes(ids)                       // HTTP interface client → restaurant-service
            .collectList()
            .map(quotes -> Pricing.price(command.restaurantId(), command.items(), quotes))  // pure
            .map(priced -> new Order(... PENDING_PAYMENT ...))
            .flatMap(store::insert)                  // reactive transaction over 2 tables
            .flatMap(this::charge);                  // payment → status → Kafka → SSE watchers
}
```

`Mono.defer` matters: without it, `createAndPay(...)` would be *assembled* (and the catalog call
prepared) even for replays.

## Reactive transaction across two tables

```java
@Override
@Transactional
public Mono<Order> insert(Order order) {
    return orders.save(entity).flatMap(saved -> lines
            .saveAll(order.lines().stream().map(l -> new OrderLineEntity(null, saved.id(), ...)).toList())
            .then(Mono.just(toDomain(saved).withLines(order.lines()))));
}
```

## Payment call: timeout + retry + circuit breaker

```java
--8<-- "food-delivery/order-service/src/main/java/com/example/fooddelivery/order/infrastructure/HttpPayments.java:charge"
```

```java
--8<-- "food-delivery/order-service/src/main/java/com/example/fooddelivery/order/config/OrderServiceConfig.java:breaker"
```

Retrying a payment is only safe because the `Idempotency-Key` travels with every attempt and
payment-service replays the first result.

## State machine guards against out-of-order events

```java
case PAID -> EnumSet.of(COURIER_ASSIGNED, PICKED_UP, IN_TRANSIT, DELIVERED);
case IN_TRANSIT -> EnumSet.of(IN_TRANSIT, DELIVERED);
case DELIVERED, PAYMENT_FAILED -> EnumSet.noneOf(OrderStatus.class);
```

A late `PICKED_UP` arriving after `IN_TRANSIT` is ignored instead of moving the order backwards.

## Kafka

- **Producer:** `Mono.fromFuture(() -> kafka.send(topic, orderId, json))` — non-blocking.
- **Consumer:** `@KafkaListener` returning `Mono<Void>` — offset committed when the DB update completes.

## Live status over SSE

```java
--8<-- "food-delivery/order-service/src/main/java/com/example/fooddelivery/order/api/OrderController.java:sse"
```

`orders.watch(id)` emits the current status, then live updates from a
`Sinks.many().multicast().directBestEffort()` filtered by order id, and completes on a terminal status —
which also stops the heartbeat.
