# WebClient

`WebClient` is the non-blocking, streaming HTTP client (Reactor Netty underneath). In Boot, inject the
auto-configured **`WebClient.Builder`** (from `spring-boot-webclient`) — it carries codecs,
observation/tracing and customizers — and build one client per downstream service.

```java
@Configuration
class ClientsConfig {
    @Bean
    WebClient inventoryWebClient(WebClient.Builder builder) {
        return builder.clone()                         // don't mutate the shared builder
                .baseUrl("http://inventory:8080")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    InventoryClient inventoryClient(WebClient inventoryWebClient) {
        return new InventoryClient(inventoryWebClient);
    }
}
```

## A production-shaped call

```java
--8<-- "playbook-examples/src/main/java/com/example/reactive/playbook/web/InventoryClient.java:webclient"
```

## `retrieve()` vs `exchangeToMono()`

| | `retrieve()` | `exchangeToMono(resp -> ...)` |
|---|---|---|
| Error statuses | become `WebClientResponseException` (customise with `onStatus`) | you inspect `resp.statusCode()` yourself |
| Body handling | `bodyToMono`, `bodyToFlux`, `toEntity`, `toBodilessEntity` | you **must** consume or release the body |
| Use | 95% of calls | status-dependent decoding |

## Streaming responses

```java
Flux<PriceTick> ticks = webClient.get().uri("/products/{id}/prices", id)
        .accept(MediaType.TEXT_EVENT_STREAM)
        .retrieve()
        .bodyToFlux(PriceTick.class);          // elements arrive as the server emits them
```

## Timeouts at every layer

```java
HttpClient http = HttpClient.create(ConnectionProvider.builder("inventory")
                .maxConnections(200)
                .pendingAcquireTimeout(Duration.ofSeconds(5))
                .maxIdleTime(Duration.ofSeconds(30))
                .build())
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2_000)       // TCP connect
        .responseTimeout(Duration.ofSeconds(3));                   // first response byte
WebClient client = builder.clientConnector(new ReactorClientHttpConnector(http)).build();
// …and the operator-level .timeout(...) for the whole Mono, including decoding and retries.
```

## Filters (`ExchangeFilterFunction`)

```java
ExchangeFilterFunction logRequest = ExchangeFilterFunction.ofRequestProcessor(req -> {
    log.info("{} {}", req.method(), req.url());
    return Mono.just(req);
});
WebClient client = builder.filter(logRequest).build();
```

Common filters: auth header injection / OAuth2 token relay (see [OAuth2](../security/oauth2-jwt.md)),
correlation-id propagation, metrics.

## Testing without a server

Stub the transport with an `ExchangeFunction`:

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/web/WebFluxExamplesTest.java:webClientTest"
```

For full HTTP fidelity use OkHttp's `MockWebServer` or WireMock.
