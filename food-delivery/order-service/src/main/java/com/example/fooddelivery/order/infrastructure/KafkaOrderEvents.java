package com.example.fooddelivery.order.infrastructure;

import com.example.fooddelivery.events.OrderPlaced;
import com.example.fooddelivery.events.Topics;
import com.example.fooddelivery.order.domain.Ports.OrderEvents;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

/**
 * Publishes to Kafka without blocking: {@code KafkaTemplate.send} returns a CompletableFuture that we adapt
 * with {@code Mono.fromFuture}. The order id is the record key, so all events of one order stay in order
 * on a single partition.
 *
 * <p>Production note: persisting the event to an outbox table in the same R2DBC transaction (relayed by
 * Debezium or a poller) removes the "DB committed but Kafka send failed" window.
 */
@Component
class KafkaOrderEvents implements OrderEvents {

    private final KafkaTemplate<String, String> kafka;
    private final JsonMapper json;

    KafkaOrderEvents(KafkaTemplate<String, String> kafka, JsonMapper json) {
        this.kafka = kafka;
        this.json = json;
    }

    @Override
    public Mono<Void> orderPlaced(OrderPlaced event) {
        return Mono.fromCallable(() -> json.writeValueAsString(event))
                .flatMap(payload -> Mono.fromFuture(() -> kafka.send(Topics.ORDER_PLACED, event.orderId(), payload)))
                .then();
    }
}
