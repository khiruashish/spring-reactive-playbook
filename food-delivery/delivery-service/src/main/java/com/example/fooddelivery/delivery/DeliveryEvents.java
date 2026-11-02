package com.example.fooddelivery.delivery;

import com.example.fooddelivery.events.DeliveryStatusChanged;
import com.example.fooddelivery.events.Topics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.json.JsonMapper;

/** Publishes every delivery change to Kafka (for other services) and to a local hot stream (for SSE). */
@Component
public class DeliveryEvents {

    private final KafkaTemplate<String, String> kafka;
    private final JsonMapper json;
    private final Sinks.Many<DeliveryStatusChanged> local = Sinks.many().multicast().directBestEffort();

    public DeliveryEvents(KafkaTemplate<String, String> kafka, JsonMapper json) {
        this.kafka = kafka;
        this.json = json;
    }

    public Mono<Void> publish(DeliveryStatusChanged event) {
        return Mono.fromCallable(() -> json.writeValueAsString(event))
                .flatMap(payload -> Mono.fromFuture(() -> kafka.send(Topics.DELIVERY_STATUS, event.orderId(), payload)))
                .doOnSuccess(sent -> local.tryEmitNext(event))
                .then();
    }

    public Flux<DeliveryStatusChanged> live() {
        return local.asFlux();
    }
}
