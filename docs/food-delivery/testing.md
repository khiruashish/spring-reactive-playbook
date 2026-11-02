# Testing the App

All unit and slice tests run **without Docker**:

```bash
./gradlew :food-delivery:restaurant-service:test :food-delivery:order-service:test \
          :food-delivery:payment-service:test :food-delivery:delivery-service:test \
          :food-delivery:notification-service:test :food-delivery:api-gateway:test
```

| Test | Technique |
|---|---|
| `RestaurantRouterTest` | `WebTestClient.bindToRouterFunction` + Mockito repositories |
| `PricingTest` | pure domain rules + state machine |
| `OrderServiceTest` | whole use case against in-memory port fakes, `StepVerifier`, live `watch()` stream |
| `PaymentGatewayTest` | `withVirtualTime`, failure injection, idempotent replay |
| `CourierSimulatorTest` | `withVirtualTime` over a minutes-long trip |
| `NotificationHubTest` | replay sink semantics for late subscribers |
| `SecurityConfigTest` | dev token round-trips through the JWT decoder |

## The ports make the core testable

```java
private OrderService service(Payments payments) {
    return new OrderService(store, catalog, payments, e -> Mono.fromRunnable(() -> published.add(e)), stream,
            Clock.fixed(Instant.parse("2026-11-02T10:00:00Z"), ZoneOffset.UTC));
}

@Test
void paymentOutageMarksOrderFailedAndPublishesNothing() {
    Payments down = (k, r, c, a) -> Mono.error(new IllegalStateException("payment unavailable"));
    StepVerifier.create(service(down).place("k-2", order()))
            .assertNext(o -> assertThat(o.status()).isEqualTo(OrderStatus.PAYMENT_FAILED))
            .verifyComplete();
    assertThat(published).isEmpty();
}
```

Ports are functional interfaces, so fakes are one-line lambdas.

## Going further

Add Testcontainers ITs per service (PostgreSQL + Kafka for order-service, MongoDB + Kafka for
delivery-service) using `@ServiceConnection` — see [Integration Tests](../testing/integration-testing.md) —
and a smoke test that drives the gateway exactly like the [curl walkthrough](running.md).
