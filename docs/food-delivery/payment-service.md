# Payment Service

**Role:** a fake card processor that behaves like a real one *badly*: latency, random outages, and
declines above a limit. **Topics:** [Creating Sequences](../reactor/creating-sequences.md),
[Hot vs Cold](../reactor/hot-vs-cold-and-sinks.md), [StepVerifier](../testing/stepverifier.md).

```java
private Mono<Payment> process(ChargeRequest request) {
    return Mono.delay(props.latency())                       // non-blocking latency
            .flatMap(tick -> random.getAsDouble() < props.failureRate()
                    ? Mono.error(new ResponseStatusException(SERVICE_UNAVAILABLE, "processor unavailable"))
                    : Mono.just(new Payment(..., amount > limit ? DECLINED : CAPTURED, ...)));
}
```

## Idempotency with `Mono.cache()`

```java
public Mono<Payment> charge(String idempotencyKey, ChargeRequest request) {
    return byIdempotencyKey.computeIfAbsent(idempotencyKey, key -> process(request)
            .doOnError(e -> byIdempotencyKey.remove(key))    // don't cache failures
            .cache());                                       // replay the first success to every retry
}
```

A cached `Mono` is *hot after first subscription*: the second request with the same key gets the same
`paymentId` without re-charging. (In production this map would be a database table with a unique key.)

## Tests use virtual time

```java
StepVerifier.withVirtualTime(() -> new PaymentGateway(props(0), () -> 1.0)
                .charge(null, new ChargeRequest("o-1", "c-1", 45_000)))
        .expectSubscription()
        .expectNoEvent(Duration.ofSeconds(2))          // the 2 s latency passes instantly
        .assertNext(p -> assertThat(p.status()).isEqualTo(Status.CAPTURED))
        .verifyComplete();
```

Set `PAYMENT_FAILURE_RATE` to `0.8` to watch order-service retry and then open its circuit breaker.
