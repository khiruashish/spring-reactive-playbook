# Observability

```kotlin
implementation("org.springframework.boot:spring-boot-starter-actuator")
implementation("io.micrometer:micrometer-registry-prometheus")
implementation("org.springframework.boot:spring-boot-starter-opentelemetry")  // Boot 4: OTel tracing + OTLP export
```

## Metrics you get for free

| Source | Metric |
|---|---|
| WebFlux server | `http.server.requests` (timer: method, uri template, status, outcome) |
| WebClient | `http.client.requests` |
| R2DBC pool | `r2dbc.pool.acquired`, `.pending`, `.idle` |
| Reactor Netty | `reactor.netty.*` (enable with `HttpClient.metrics(true, uriMapper)`) |
| Kafka | `kafka.producer.*`, `kafka.consumer.*`, `spring.kafka.listener` |
| Resilience4j | `resilience4j.circuitbreaker.state`, `.calls` |

## Observing your own pipelines

```java
Mono<Quote> quote = pricingClient.quote(id)
        .name("pricing.quote")              // metric/observation name
        .tag("source", "pricing-service")
        .tap(Micrometer.observation(observationRegistry));   // timer + span for this Mono
```

`tap(Micrometer.observation(...))` comes from `io.projectreactor:reactor-core-micrometer`.

## Tracing and context propagation

Each request hops threads, so `ThreadLocal`-based tracing and MDC need help. Boot wires Micrometer's
**context-propagation** library into Reactor:

```yaml
spring:
  reactor:
    context-propagation: auto       # Hooks.enableAutomaticContextPropagation()
management:
  tracing:
    sampling:
      probability: 1.0              # sample everything in dev; lower in production
  otlp:
    tracing:
      endpoint: http://otel-collector:4318/v1/traces
```

With that, `traceId`/`spanId` are restored into the MDC around every operator, so plain
`log.info(...)` inside `map`/`flatMap` prints the right trace id, and `WebClient`/Kafka calls carry the
`traceparent` header automatically.

```yaml
logging:
  pattern:
    correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}] "
```

## Health

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true          # /actuator/health/liveness and /readiness for Kubernetes
      show-details: when-authorized
```

Reactive health indicators (`ReactiveHealthIndicator`) exist for R2DBC, Mongo, Redis — they never block
the event loop.

## What to put on the dashboard

- request rate, error rate, p95/p99 latency per route (RED)
- event-loop saturation: pending tasks, `reactor.netty.eventloop.pending.tasks`
- pool usage: R2DBC pending acquisitions, WebClient connection pool pending
- `boundedElastic` active/queued tasks — growth means blocking work piling up
- Kafka consumer lag, circuit breaker states
