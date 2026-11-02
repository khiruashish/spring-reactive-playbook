package com.example.reactive.playbook.reactor;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class CreatingSequencesTest {

    @Test
    void monoAndFluxBasics() {
        // --8<-- [start:basics]
        Mono<String> one = Mono.just("hello");               // 0..1 element
        Mono<String> none = Mono.empty();                    // completes with no element
        Mono<String> failed = Mono.error(new IllegalStateException("boom"));
        Flux<Integer> many = Flux.just(1, 2, 3);             // 0..N elements
        Flux<Integer> range = Flux.range(10, 3);             // 10, 11, 12
        Flux<String> fromList = Flux.fromIterable(List.of("a", "b"));

        StepVerifier.create(one).expectNext("hello").verifyComplete();
        StepVerifier.create(none).verifyComplete();
        StepVerifier.create(failed).verifyErrorMessage("boom");
        StepVerifier.create(many).expectNext(1, 2, 3).verifyComplete();
        StepVerifier.create(range).expectNext(10, 11, 12).verifyComplete();
        StepVerifier.create(fromList).expectNext("a", "b").verifyComplete();
        // --8<-- [end:basics]
    }

    @Test
    void nothingHappensUntilSubscribe() {
        // --8<-- [start:lazy]
        AtomicInteger calls = new AtomicInteger();
        Mono<Integer> lazy = Mono.fromCallable(calls::incrementAndGet); // assembly: just a recipe

        assertThat(calls).hasValue(0);           // nothing ran yet
        lazy.block();                            // subscription triggers execution
        lazy.block();                            // ...every time (cold publisher)
        assertThat(calls).hasValue(2);
        // --8<-- [end:lazy]
    }

    @Test
    void justIsEagerDeferIsLazy() {
        // --8<-- [start:defer]
        AtomicInteger counter = new AtomicInteger();
        Mono<Integer> eager = Mono.just(counter.incrementAndGet());            // evaluated NOW, once
        Mono<Integer> lazy = Mono.defer(() -> Mono.just(counter.incrementAndGet())); // per subscription

        StepVerifier.create(eager).expectNext(1).verifyComplete();
        StepVerifier.create(eager).expectNext(1).verifyComplete();             // same captured value
        StepVerifier.create(lazy).expectNext(2).verifyComplete();
        StepVerifier.create(lazy).expectNext(3).verifyComplete();
        // --8<-- [end:defer]
    }

    @Test
    void generateIsSynchronousAndStateful() {
        // --8<-- [start:generate]
        // Fibonacci: one sink.next per round, state carried between rounds
        Flux<Long> fibonacci = Flux.generate(
                () -> new long[] {0, 1},
                (state, sink) -> {
                    sink.next(state[0]);
                    return new long[] {state[1], state[0] + state[1]};
                });

        StepVerifier.create(fibonacci.take(7))
                .expectNext(0L, 1L, 1L, 2L, 3L, 5L, 8L)
                .verifyComplete();
        // --8<-- [end:generate]
    }

    @Test
    void createBridgesCallbackApis() {
        // --8<-- [start:create]
        // Flux.create adapts listener/callback APIs; it may emit from any thread, many at once
        Flux<String> events = Flux.create(sink -> {
            PriceTicker ticker = new PriceTicker();
            ticker.onPrice(sink::next);
            ticker.onClose(sink::complete);
            sink.onDispose(ticker::stop);         // cleanup when the subscriber cancels
            ticker.start();
        }, FluxSink.OverflowStrategy.BUFFER);

        StepVerifier.create(events).expectNext("AAPL 190", "AAPL 191").verifyComplete();
        // --8<-- [end:create]
    }

    @Test
    void timeBasedSources() {
        // --8<-- [start:interval]
        StepVerifier.withVirtualTime(() -> Flux.interval(Duration.ofSeconds(1)).take(3))
                .expectSubscription()
                .thenAwait(Duration.ofSeconds(3))
                .expectNext(0L, 1L, 2L)
                .verifyComplete();
        // --8<-- [end:interval]
    }

    @Test
    void fromOptionalAndFuture() {
        StepVerifier.create(Mono.justOrEmpty(java.util.Optional.empty())).verifyComplete();
        StepVerifier.create(Mono.fromFuture(() -> java.util.concurrent.CompletableFuture.completedFuture(7)))
                .expectNext(7).verifyComplete();
    }

    /** A toy callback-based API used to demonstrate {@code Flux.create}. */
    static final class PriceTicker {
        private java.util.function.Consumer<String> listener = s -> { };
        private Runnable closer = () -> { };

        void onPrice(java.util.function.Consumer<String> l) {
            listener = l;
        }

        void onClose(Runnable r) {
            closer = r;
        }

        void start() {
            listener.accept("AAPL 190");
            listener.accept("AAPL 191");
            closer.run();
        }

        void stop() {
        }
    }
}
