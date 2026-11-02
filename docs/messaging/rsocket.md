# RSocket

[RSocket](https://rsocket.io/) is a binary, multiplexed application protocol (over TCP or WebSocket)
with **Reactive Streams semantics on the wire** — `request(n)` crosses the network.

| Interaction | Shape | Example |
|---|---|---|
| request-response | 1 → 1 | get a product |
| fire-and-forget | 1 → 0 | log/metric event |
| request-stream | 1 → N | price feed |
| request-channel | N → N | bidirectional chat, telemetry |

```kotlin
implementation("org.springframework.boot:spring-boot-starter-rsocket")
```

```yaml
spring.rsocket.server.port: 7000
```

## Server

```java
@Controller
class PriceRSocketController {

    @MessageMapping("product.{id}")
    Mono<Product> product(@DestinationVariable String id) { return repository.findById(id); }

    @MessageMapping("prices")
    Flux<PriceTick> prices(String productId) {
        return Flux.interval(Duration.ofSeconds(1)).map(i -> new PriceTick(productId, 1_000 + i, i));
    }

    @MessageMapping("audit")
    Mono<Void> audit(AuditEvent event) { return auditLog.save(event).then(); }  // fire-and-forget

    @MessageMapping("chat")
    Flux<String> chat(Flux<String> inbound) { return inbound.map(m -> "echo: " + m); }
}
```

## Client

```java
RSocketRequester requester = builder.tcp("localhost", 7000);

Flux<PriceTick> ticks = requester.route("prices").data("p1").retrieveFlux(PriceTick.class);
Mono<Void> sent = requester.route("audit").data(event).send();
```

RSocket also supports `@RSocketExchange` interface clients (like HTTP interface clients), resumption
after disconnects, and lease-based load control. It is a strong fit for service-to-service streaming
inside a platform; for browser-facing APIs, SSE/WebSocket over HTTP are easier to operate.
