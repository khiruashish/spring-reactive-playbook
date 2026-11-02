package com.example.fooddelivery.delivery;

import com.example.fooddelivery.events.DeliveryStatusChanged;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    private final DeliveryService deliveries;

    public DeliveryController(DeliveryService deliveries) {
        this.deliveries = deliveries;
    }

    @GetMapping("/{orderId}")
    public Mono<ResponseEntity<Delivery>> get(@PathVariable String orderId) {
        return deliveries.forOrder(orderId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /** Live courier tracking as an NDJSON/SSE stream — each element is flushed as soon as it is emitted. */
    @GetMapping(path = "/{orderId}/track", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<DeliveryStatusChanged> track(@PathVariable String orderId) {
        return deliveries.track(orderId);
    }
}
