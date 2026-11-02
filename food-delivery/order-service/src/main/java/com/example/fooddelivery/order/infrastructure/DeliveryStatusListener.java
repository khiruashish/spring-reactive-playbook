package com.example.fooddelivery.order.infrastructure;

import com.example.fooddelivery.events.DeliveryStatusChanged;
import com.example.fooddelivery.events.Topics;
import com.example.fooddelivery.order.application.OrderService;
import com.example.fooddelivery.order.domain.OrderStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

/**
 * A listener that returns {@code Mono<Void>}: Spring Kafka treats it as an async result and commits the
 * offset only when the Mono completes — the listener thread is never blocked on the database.
 */
@Component
class DeliveryStatusListener {

    private final OrderService orders;
    private final JsonMapper json;

    DeliveryStatusListener(OrderService orders, JsonMapper json) {
        this.orders = orders;
        this.json = json;
    }

    @KafkaListener(topics = Topics.DELIVERY_STATUS, groupId = "order-service")
    public Mono<Void> onDeliveryStatus(String payload) {
        return Mono.fromCallable(() -> json.readValue(payload, DeliveryStatusChanged.class))
                .flatMap(e -> orders.applyDeliveryUpdate(Long.parseLong(e.orderId()),
                        OrderStatus.valueOf(e.status().name()), e.latitude(), e.longitude(), e.at()));
    }
}
