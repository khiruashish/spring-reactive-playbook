package com.example.fooddelivery.order.application;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * In-process hot stream of status changes, fanned out to every SSE subscriber.
 *
 * <p>{@code multicast().directBestEffort()}: no replay, and a slow subscriber drops updates instead of
 * stalling the others. Scaled to N instances, each instance consumes the Kafka topic with its own
 * consumer group (or uses Redis pub/sub) so every instance sees every update.
 */
@Component
public class OrderStatusStream {

    private final Sinks.Many<OrderStatusUpdate> sink = Sinks.many().multicast().directBestEffort();

    public void publish(OrderStatusUpdate update) {
        // tryEmitNext is non-throwing; FAIL_NON_SERIALIZED can happen under concurrent emitters,
        // so emitNext with a busy-looping handler serialises them safely.
        sink.emitNext(update, Sinks.EmitFailureHandler.busyLooping(java.time.Duration.ofMillis(50)));
    }

    public Flux<OrderStatusUpdate> updatesFor(long orderId) {
        return sink.asFlux().filter(u -> u.orderId() == orderId);
    }
}
