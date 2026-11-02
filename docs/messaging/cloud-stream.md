# Spring Cloud Stream (Reactive Functions)

Spring Cloud Stream binds plain `java.util.function` beans to brokers (Kafka, RabbitMQ, Pulsar…). With
reactive signatures, a function receives the **whole stream** as a `Flux`.

```kotlin
implementation("org.springframework.cloud:spring-cloud-stream-binder-kafka")
```

```java
@Configuration
class PricePipeline {

    /** price-in → enrich → price-out, as one reactive pipeline. */
    @Bean
    Function<Flux<PriceChanged>, Flux<EnrichedPrice>> enrich(FxRates fx) {
        return prices -> prices
                .filter(p -> p.priceMinor() > 0)
                .concatMap(p -> fx.rate(p.currency())
                        .map(r -> EnrichedPrice.of(p, r))
                        .onErrorResume(e -> {                 // per-element recovery: one bad message
                            log.warn("skipped {}", p, e);     // must not terminate the whole stream
                            return Mono.empty();
                        }));
    }

    @Bean
    Supplier<Flux<Heartbeat>> heartbeat() {               // reactive source: called once
        return () -> Flux.interval(Duration.ofSeconds(5)).map(Heartbeat::new);
    }

    @Bean
    Consumer<Flux<EnrichedPrice>> audit(AuditRepository repo) {
        return flux -> flux.concatMap(repo::save).subscribe();   // reactive consumers must subscribe
    }
}
```

```yaml
spring:
  cloud:
    function:
      definition: enrich;heartbeat;audit
    stream:
      bindings:
        enrich-in-0:  { destination: prices.raw,      group: enricher }
        enrich-out-0: { destination: prices.enriched }
        audit-in-0:   { destination: prices.enriched, group: audit }
```

!!! warning "Trade-offs of reactive functions"
    With `Function<Flux, Flux>` the framework hands you the stream **once**: per-message retries,
    DLQ and acknowledgement are your responsibility (an error terminates the Flux). For most
    business consumers, imperative `Function<In, Out>` (or `Function<In, Mono<Out>>`) with the binder's
    retry/DLQ support is simpler and safer. Reactive functions shine for stream transformations
    (windowing, aggregation, sampling).
