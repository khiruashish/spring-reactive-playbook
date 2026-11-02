# Spring Cloud Gateway

Spring Cloud Gateway (server-webflux variant) is a reactive API gateway built on WebFlux + Reactor Netty:
routing, security, rate limiting, retries and observability at the edge.

```kotlin
implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2025.1.4"))
implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")
```

## Routes in YAML

!!! info "Property prefix"
    Since Gateway 4.3 the properties live under `spring.cloud.gateway.server.webflux.*`
    (the old `spring.cloud.gateway.*` names are deprecated).

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: catalog
              uri: http://catalog:8080
              predicates:
                - Path=/api/products/**
                - Method=GET,POST
              filters:
                - StripPrefix=1
                - name: Retry
                  args: { retries: 2, methods: GET, statuses: BAD_GATEWAY,SERVICE_UNAVAILABLE }
                - name: CircuitBreaker
                  args: { name: catalog, fallbackUri: forward:/fallback/catalog }
                - name: RequestRateLimiter
                  args:
                    redis-rate-limiter.replenishRate: 10
                    redis-rate-limiter.burstCapacity: 20
                    key-resolver: "#{@userKeyResolver}"
            - id: live
              uri: ws://notifications:8080        # WebSocket routes just work
              predicates: [ Path=/ws/** ]
```

## Routes in Java

```java
@Bean
RouteLocator routes(RouteLocatorBuilder builder) {
    return builder.routes()
            .route("reviews", r -> r.path("/api/reviews/**")
                    .filters(f -> f.addRequestHeader("X-Source", "gateway").circuitBreaker(c -> c.setName("reviews")))
                    .uri("http://reviews:8080"))
            .build();
}
```

## Global filters — your own edge logic

```java
@Component
class TimingFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.nanoTime();
        return chain.filter(exchange).doFinally(s -> log.info("{} took {} ms",
                exchange.getRequest().getPath(), (System.nanoTime() - start) / 1_000_000));
    }
    @Override public int getOrder() { return Ordered.HIGHEST_PRECEDENCE; }
}
```

The capstone gateway uses global filters to **strip spoofable identity headers and relay the
authenticated user** to downstream services — see [API Gateway](../food-delivery/api-gateway.md).

## Gateway responsibilities checklist

- TLS termination, authentication (JWT validation), coarse authorization
- Routing and versioning, request/response header hygiene
- Rate limiting, request size limits, timeouts (`spring.cloud.gateway.server.webflux.httpclient.response-timeout`)
- Retries **only for idempotent** methods, circuit breakers with fallbacks
- Correlation ids, access logs, metrics and tracing
