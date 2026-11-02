# HTTP Interface Clients

Declare an interface; Spring generates the implementation on top of `WebClient` (or `RestClient`).
Return reactive types and the proxy is fully non-blocking.

```java
@HttpExchange("/api/products")
public interface ProductApi {

    @GetExchange
    Flux<Product> list(@RequestParam(required = false) String category);

    @GetExchange("/{id}")
    Mono<Product> get(@PathVariable String id);

    @PostExchange
    Mono<ResponseEntity<Product>> create(@RequestBody Product product);

    @DeleteExchange("/{id}")
    Mono<Void> delete(@PathVariable String id);
}
```

## Creating the proxy

```java
@Bean
ProductApi productApi(WebClient.Builder builder) {
    WebClient client = builder.clone().baseUrl("http://catalog:8080").build();
    return HttpServiceProxyFactory.builderFor(WebClientAdapter.create(client))
            .build()
            .createClient(ProductApi.class);
}
```

Spring Framework 7 / Boot 4 can also register clients declaratively with `@ImportHttpServices` and
configure base URLs per group via `spring.http.serviceclient.<group>.base-url`.

## Resilience stays explicit

The proxy returns plain `Mono`/`Flux`, so you compose timeouts, retries and circuit breakers exactly as
with `WebClient`:

```java
productApi.get(id)
        .timeout(Duration.ofSeconds(2))
        .retryWhen(Retry.backoff(2, Duration.ofMillis(100)));
```

!!! note "Collections as query parameters"
    A `Collection` argument for `@RequestParam("ids")` is sent as **repeated** parameters
    (`?ids=1&ids=2`), not comma-separated. Servers must read *all* values (`queryParams().get("ids")`).
    The capstone hit exactly this — see [Restaurant Service](../food-delivery/restaurant-service.md).
