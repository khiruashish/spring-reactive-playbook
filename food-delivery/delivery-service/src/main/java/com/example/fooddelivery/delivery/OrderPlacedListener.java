package com.example.fooddelivery.delivery;

import com.example.fooddelivery.events.OrderPlaced;
import com.example.fooddelivery.events.Topics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

@Component
class OrderPlacedListener {

    private final DeliveryService deliveries;
    private final JsonMapper json;

    OrderPlacedListener(DeliveryService deliveries, JsonMapper json) {
        this.deliveries = deliveries;
        this.json = json;
    }

    /** Async listener: the offset is committed when the returned Mono completes. */
    @KafkaListener(topics = Topics.ORDER_PLACED, groupId = "delivery-service")
    public Mono<Void> onOrderPlaced(String payload) {
        return Mono.fromCallable(() -> json.readValue(payload, OrderPlaced.class))
                .flatMap(deliveries::onOrderPlaced)
                .then();
    }
}
