package com.example.reactive.playbook.web;

import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class PriceStreamController {

    public record PriceTick(String productId, long priceMinor, long sequence) {
    }

    // --8<-- [start:sse]
    /** Server-Sent Events: an infinite stream; cancelled automatically when the client disconnects. */
    @GetMapping(path = "/products/{id}/prices", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<PriceTick>> prices(@PathVariable String id) {
        return Flux.interval(Duration.ofSeconds(1))
                .map(seq -> new PriceTick(id, 1_000 + (seq % 5) * 10, seq))
                .map(tick -> ServerSentEvent.builder(tick)
                        .id(String.valueOf(tick.sequence()))
                        .event("price")
                        .build());
    }

    /** NDJSON: one JSON document per line — great for service-to-service streaming. */
    @GetMapping(path = "/products/{id}/prices.ndjson", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<PriceTick> pricesNdjson(@PathVariable String id) {
        return Flux.interval(Duration.ofMillis(100)).take(3).map(seq -> new PriceTick(id, 1_000, seq));
    }
    // --8<-- [end:sse]
}
