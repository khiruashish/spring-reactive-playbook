package com.example.reactive.playbook.codinginterview;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Problem 5: write events to the DB in batches of up to N, or every T ms, whichever comes first, one batch in flight at a time. */
public class P05Batcher {

    public record Event(int id) { }

    public static Flux<Integer> batchWrite(Flux<Event> events, int maxSize, Duration maxWait, Function<List<Event>, Mono<Integer>> bulkInsert) {
        return events
                .bufferTimeout(maxSize, maxWait)          // size OR time triggers a batch
                .concatMap(bulkInsert);                   // sequential writes: ordering + bounded DB load
    }
}
