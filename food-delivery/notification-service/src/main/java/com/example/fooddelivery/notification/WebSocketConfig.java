package com.example.fooddelivery.notification;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;

@Configuration
class WebSocketConfig {

    /** Maps the WebSocket endpoint; order -1 puts it ahead of annotated controllers. */
    @Bean
    HandlerMapping webSocketMapping(NotificationWebSocketHandler handler) {
        return new SimpleUrlHandlerMapping(Map.of("/ws/notifications", handler), -1);
    }

    /** SSE fallback for clients/proxies without WebSocket support. */
    @Bean
    RouterFunction<ServerResponse> sseNotifications(NotificationHub hub) {
        return RouterFunctions.route()
                .GET("/api/notifications/stream", request -> ServerResponse.ok()
                        .contentType(MediaType.TEXT_EVENT_STREAM)
                        .body(hub.stream(request.queryParam("orderId").orElse(null))
                                .map(n -> ServerSentEvent.builder(n).event(n.type()).build()),
                                ServerSentEvent.class))
                .build();
    }
}
