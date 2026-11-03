package com.example.reactive.playbook.codinginterview;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class P05BatcherTest {

    @Test
    void burstFlushesBySize_trickleFlushesByTime() {
        List<Integer> batchSizes = new CopyOnWriteArrayList<>();
        StepVerifier.withVirtualTime(() -> {
                    Flux<P05Batcher.Event> events = Flux.concat(
                            Flux.range(1, 25).map(P05Batcher.Event::new),                                           // burst
                            Flux.interval(Duration.ofSeconds(5)).take(3).map(i -> new P05Batcher.Event(26 + i.intValue())));  // trickle
                    return P05Batcher.batchWrite(events, 10, Duration.ofSeconds(1), batch -> {
                        batchSizes.add(batch.size());
                        return Mono.just(batch.size());
                    });
                })
                .expectNext(10, 10)                          // size-triggered, immediately
                .thenAwait(Duration.ofSeconds(1)).expectNext(5)   // time-triggered remainder of the burst
                .thenAwait(Duration.ofSeconds(30)).expectNext(1, 1, 1)
                .verifyComplete();
        assertThat(batchSizes).containsExactly(10, 10, 5, 1, 1, 1);
    }

    @Test
    void oneBatchInFlightAtATime_preservesOrder() {
        AtomicInteger inFlight = new AtomicInteger(), maxInFlight = new AtomicInteger();
        List<Integer> firstIds = new CopyOnWriteArrayList<>();
        StepVerifier.withVirtualTime(() -> P05Batcher.batchWrite(Flux.range(1, 30).map(P05Batcher.Event::new), 10, Duration.ofSeconds(1),
                        batch -> Mono.delay(Duration.ofMillis(300))
                                .doOnSubscribe(s -> { maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max); firstIds.add(batch.get(0).id()); })
                                .doOnTerminate(inFlight::decrementAndGet)
                                .thenReturn(batch.size())))
                .thenAwait(Duration.ofSeconds(5)).expectNext(10, 10, 10).verifyComplete();
        assertThat(maxInFlight.get()).isEqualTo(1);
        assertThat(firstIds).containsExactly(1, 11, 21);
    }
}
