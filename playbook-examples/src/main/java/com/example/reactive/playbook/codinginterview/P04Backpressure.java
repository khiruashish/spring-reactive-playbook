package com.example.reactive.playbook.codinginterview;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.Flux;

/** Problem 4: a producer that ignores demand (sensor/clicks) vs a slow consumer: buffer, drop or keep latest. */
public class P04Backpressure {

    public final List<Integer> dropped = new CopyOnWriteArrayList<>();

    /** Keep up to `size` waiting items; beyond that, drop the OLDEST and record it (bounded memory, lossy). */
    public Flux<Integer> bounded(Flux<Integer> hot, int size) {
        return hot.onBackpressureBuffer(size, dropped::add, BufferOverflowStrategy.DROP_OLDEST);
    }

    /** Drop whatever arrives while the consumer has no outstanding demand. */
    public Flux<Integer> drop(Flux<Integer> hot) { return hot.onBackpressureDrop(dropped::add); }

    /** Only the most recent value matters (price, position): keep the latest, discard older unconsumed ones. */
    public Flux<Integer> latest(Flux<Integer> hot) { return hot.onBackpressureLatest(); }

    /** A cold source that DOES honour demand: ask in batches of 10 with a 75% replenish threshold. */
    public Flux<Integer> paced(Flux<Integer> cold) { return cold.limitRate(10); }
}
