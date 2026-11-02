package com.example.fooddelivery.notification;

import java.time.Duration;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Fan-out hub between Kafka consumers (producers of notifications) and WebSocket sessions (consumers).
 * {@code replay().limit(Duration)} lets a client that connects a moment after ordering still see
 * the last 30 seconds of notifications.
 */
@Component
public class NotificationHub {

    private final Sinks.Many<Notification> sink = Sinks.many().replay().limit(Duration.ofSeconds(30));

    public void push(Notification notification) {
        sink.emitNext(notification, Sinks.EmitFailureHandler.busyLooping(Duration.ofMillis(50)));
    }

    public Flux<Notification> stream(String orderId) {
        Flux<Notification> all = sink.asFlux();
        return orderId == null ? all : all.filter(n -> n.orderId().equals(orderId));
    }
}
