package com.example.reactive.playbook.codinginterview;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.test.StepVerifier;

class P04BackpressureTest {

    /** A producer that pushes 100 items immediately and ignores demand (like UI events or a sensor). */
    static Flux<Integer> overproducing() {
        return Flux.create(sink -> { for (int i = 1; i <= 100; i++) sink.next(i); sink.complete(); }, FluxSink.OverflowStrategy.IGNORE);
    }

    /**
     * A consumer with ZERO demand while the producer runs, which requests only afterwards. (StepVerifier would send a
     * leading thenRequest() inside onSubscribe, before Flux.create starts emitting, so no overflow would ever happen.)
     */
    static final class LateSubscriber extends BaseSubscriber<Integer> {
        final List<Integer> received = new CopyOnWriteArrayList<>();
        @Override protected void hookOnSubscribe(Subscription s) { /* request nothing yet */ }
        @Override protected void hookOnNext(Integer v) { received.add(v); }
    }

    @Test
    void boundedBuffer_keepsNewest10_dropsOldest() {
        var bp = new P04Backpressure();
        var consumer = new LateSubscriber();
        bp.bounded(overproducing(), 10).subscribe(consumer);   // all 100 pushed while demand = 0
        consumer.request(3);
        assertThat(consumer.received).containsExactly(91, 92, 93);
        consumer.request(Long.MAX_VALUE);
        assertThat(consumer.received).hasSize(10).endsWith(100);
        assertThat(bp.dropped).hasSize(90).startsWith(1, 2, 3);
    }

    @Test
    void drop_deliversOnlyWhatWasRequested() {
        var bp = new P04Backpressure();
        StepVerifier.create(bp.drop(overproducing()), 5).expectNext(1, 2, 3, 4, 5).verifyComplete();
        assertThat(bp.dropped).hasSize(95);
    }

    @Test
    void latest_keepsOnlyTheMostRecentValue() {
        var consumer = new LateSubscriber();
        new P04Backpressure().latest(overproducing()).subscribe(consumer);
        consumer.request(1);
        assertThat(consumer.received).containsExactly(100);
    }

    @Test
    void limitRate_requestsInBatches_neverUnbounded() {
        List<Long> requests = new CopyOnWriteArrayList<>();
        Flux<Integer> source = Flux.range(1, 50).doOnRequest(requests::add);
        StepVerifier.create(new P04Backpressure().paced(source)).expectNextCount(50).verifyComplete();
        assertThat(requests.get(0)).isEqualTo(10);
        assertThat(requests).allMatch(r -> r <= 10);
    }
}
