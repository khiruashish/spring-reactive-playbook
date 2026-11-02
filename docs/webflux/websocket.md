# WebSocket

A reactive `WebSocketHandler` gets an inbound `Flux<WebSocketMessage>` and returns a `Mono<Void>` that
represents the whole session. Generic example — a chat room broadcasting to everyone:

```java
@Component
class ChatHandler implements WebSocketHandler {

    private final Sinks.Many<String> room = Sinks.many().multicast().directBestEffort();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Mono<Void> inbound = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(text -> room.emitNext(text, Sinks.EmitFailureHandler.busyLooping(Duration.ofMillis(50))))
                .then();

        Mono<Void> outbound = session.send(room.asFlux().map(session::textMessage));

        return Mono.zip(inbound, outbound).then();   // session lives until both complete
    }
}

@Configuration
class WebSocketConfig {
    @Bean
    HandlerMapping chatMapping(ChatHandler handler) {
        return new SimpleUrlHandlerMapping(Map.of("/ws/chat", handler), -1);  // before controllers
    }
}
```

## Patterns

| Need | Approach |
|---|---|
| server push only | `session.send(flux)`; drain `session.receive()` to notice disconnects |
| request/response over WS | correlate by message id; consider RSocket instead |
| per-user streams | read identity from the handshake (`session.getHandshakeInfo().getPrincipal()`) |
| authentication | browsers can't set headers on WS: use a cookie session or a short-lived token in the query string |
| scale-out | broadcast via Kafka/Redis so every instance sees every message |

## SSE or WebSocket?

| | SSE | WebSocket |
|---|---|---|
| Direction | server → client | both |
| Protocol | plain HTTP (proxies, HTTP/2 friendly) | upgrade to `ws://` |
| Reconnect + resume | built into `EventSource` | your code |
| Binary | no | yes |

Use SSE for notifications/feeds; WebSocket for interactive, bidirectional traffic.
