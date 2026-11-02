package com.example.reactive.playbook.reactor;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class TimeOperatorsTest {

    @Test
    void delays() {
        // --8<-- [start:delay]
        StepVerifier.withVirtualTime(() -> Flux.just("a", "b", "c").delayElements(Duration.ofSeconds(1)))
                .expectSubscription()
                .expectNoEvent(Duration.ofSeconds(1)).expectNext("a")   // one element per second
                .expectNoEvent(Duration.ofSeconds(1)).expectNext("b")
                .thenAwait(Duration.ofSeconds(1)).expectNext("c")
                .verifyComplete();

        StepVerifier.withVirtualTime(() -> Mono.just("x").delaySubscription(Duration.ofSeconds(5)))
                .expectSubscription()
                .expectNoEvent(Duration.ofSeconds(5))                   // the source isn't even subscribed yet
                .expectNext("x")
                .verifyComplete();
        // --8<-- [end:delay]
    }

    @Test
    void sampleAndThrottle() {
        // --8<-- [start:sample]
        // a value every 150ms; keep only the latest one per second (e.g. a UI update rate)
        StepVerifier.withVirtualTime(() -> Flux.interval(Duration.ofMillis(150)).take(18)
                        .sample(Duration.ofSeconds(1)))
                .thenAwait(Duration.ofSeconds(3))
                .expectNext(5L, 12L, 17L)          // latest value at t=1s, t=2s, then the final value
                .verifyComplete();

        // sampleFirst: take the first value, then ignore the rest of the window (throttle / debounce-click)
        StepVerifier.withVirtualTime(() -> Flux.interval(Duration.ofMillis(150)).take(18)
                        .sampleFirst(Duration.ofSeconds(1)))
                .thenAwait(Duration.ofSeconds(3))
                .expectNext(0L, 7L, 14L)           // emitted at 150ms, 1200ms, 2250ms
                .verifyComplete();
        // --8<-- [end:sample]
    }

    @Test
    void measuring() {
        // --8<-- [start:elapsed]
        StepVerifier.withVirtualTime(() -> Mono.just("result").delayElement(Duration.ofMillis(250)).elapsed())
                .thenAwait(Duration.ofSeconds(1))
                .assertNext(t -> {
                    assertThat(t.getT1()).isEqualTo(250L);              // milliseconds since subscription
                    assertThat(t.getT2()).isEqualTo("result");
                })
                .verifyComplete();
        // --8<-- [end:elapsed]
    }

    @Test
    void pollingWithRepeat() {
        // --8<-- [start:repeat]
        // poll a job status every 2s until it is DONE (max 10 polls)
        java.util.Iterator<String> statuses = java.util.List.of("PENDING", "RUNNING", "DONE").iterator();
        Mono<String> poll = Mono.fromSupplier(statuses::next);

        StepVerifier.withVirtualTime(() -> poll
                        .repeatWhen(done -> done.delayElements(Duration.ofSeconds(2)).take(10))
                        .takeUntil("DONE"::equals)
                        .last())
                .thenAwait(Duration.ofSeconds(10))
                .expectNext("DONE")
                .verifyComplete();
        // --8<-- [end:repeat]
    }

    @Test
    void windowByTime() {
        // --8<-- [start:windowTime]
        // events per second (a rolling counter)
        StepVerifier.withVirtualTime(() -> Flux.interval(Duration.ofMillis(300)).take(8)
                        .window(Duration.ofSeconds(1))
                        .concatMap(Flux::count))
                .thenAwait(Duration.ofSeconds(3))
                .expectNext(3L, 3L, 2L)            // events at 300..900 | 1200..1800 | 2100..2400
                .verifyComplete();
        // --8<-- [end:windowTime]
    }
}
