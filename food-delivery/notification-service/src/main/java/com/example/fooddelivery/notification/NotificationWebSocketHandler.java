package com.example.fooddelivery.notification;

import java.net.URI;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reactive WebSocket: {@code session.send(Flux)} is the outbound stream, {@code session.receive()} the
 * inbound one. The connection lives exactly as long as the returned {@code Mono<Void>}.
 */
@Component
public class NotificationWebSocketHandler implements WebSocketHandler {

    private final NotificationHub hub;
    private final JsonMapper json;

    public NotificationWebSocketHandler(NotificationHub hub, JsonMapper json) {
        this.hub = hub;
        this.json = json;
    }

    // --8<-- [start:handle]
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String orderId = queryParam(session.getHandshakeInfo().getUri(), "orderId");
        Mono<Void> outbound = session.send(hub.stream(orderId)
                .map(json::writeValueAsString)
                .map(session::textMessage));
        // drain inbound frames (client pings); when the client disconnects, receive() completes
        Mono<Void> inbound = session.receive().then();
        return Mono.firstWithSignal(outbound, inbound); // whichever side ends first closes the session
    }
    // --8<-- [end:handle]

    private static String queryParam(URI uri, String name) {
        return UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst(name);
    }
}
