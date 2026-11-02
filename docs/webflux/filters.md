# Filters

## `WebFilter` — every request

```java
--8<-- "playbook-examples/src/main/java/com/example/reactive/playbook/web/RequestIdWebFilter.java:webfilter"
```

Any operator downstream of the controller can now read the request id:

```java
Mono.deferContextual(ctx -> Mono.just("request " + ctx.get(RequestIdWebFilter.CONTEXT_KEY)))
```

!!! tip "Order"
    Spring Security's filter chain runs at order `-100` (`WebFilterChainProxy`). Use a lower number to
    run before security (e.g. request ids), a higher one to run after authentication.

## `HandlerFilterFunction` — functional routes only

See the `apiKeyRequiredForWrites()` filter in [Functional Endpoints](functional-endpoints.md).

## Typical uses

| Concern | Hook |
|---|---|
| request id / correlation id | `WebFilter` + `contextWrite` |
| authentication, authorization | Spring Security (`SecurityWebFilterChain`) |
| CORS | `CorsWebFilter` or `WebFluxConfigurer#addCorsMappings` |
| request/response logging, timing | `WebFilter` with `doFinally` |
| tenant resolution | `WebFilter` → Context |
| rate limiting | gateway `RequestRateLimiter`, or a `WebFilter` with Redis/Bucket4j |

!!! warning "Don't read the body in a filter"
    The request body is a one-shot `Flux<DataBuffer>`. Consuming it in a filter leaves nothing for the
    handler (use `ServerWebExchangeDecorator` with caching only if you really must).
