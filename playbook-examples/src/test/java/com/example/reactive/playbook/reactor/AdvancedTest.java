package com.example.reactive.playbook.reactor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class AdvancedTest {

    // --8<-- [start:customOperator]
    /** A reusable "operator": per-subscriber state, built with transformDeferred. */
    static <T> Function<Flux<T>, Flux<String>> numbered() {
        return flux -> {
            int[] counter = {0};                       // fresh state for EVERY subscriber (deferred)
            return flux.map(v -> (++counter[0]) + ". " + v);
        };
    }

    @Test
    void customOperatorWithTransformDeferred() {
        Flux<String> lines = Flux.just("alpha", "beta").transformDeferred(numbered());
        StepVerifier.create(lines).expectNext("1. alpha", "2. beta").verifyComplete();
        StepVerifier.create(lines).expectNext("1. alpha", "2. beta").verifyComplete(); // not "3. alpha"
    }
    // --8<-- [end:customOperator]

    @Test
    void jdkFlowInterop() throws Exception {
        // --8<-- [start:flowAdapter]
        // java.util.concurrent.Flow (JDK 9+) ⇄ Reactive Streams / Reactor
        try (SubmissionPublisher<String> jdkPublisher = new SubmissionPublisher<>()) {
            Flux<String> flux = JdkFlowAdapter.flowPublisherToFlux(jdkPublisher);
            StepVerifier.create(flux.take(2))
                    .then(() -> { jdkPublisher.submit("a"); jdkPublisher.submit("b"); })
                    .expectNext("a", "b")
                    .verifyComplete();
        }
        Flow.Publisher<Integer> back = JdkFlowAdapter.publisherToFlowPublisher(Flux.range(1, 3));
        assertThat(back).isNotNull();
        // --8<-- [end:flowAdapter]
    }

    @Test
    void springReactiveAdapterRegistry() {
        // --8<-- [start:adapterRegistry]
        // How Spring accepts CompletableFuture, RxJava, Mutiny, Kotlin Flow… as controller return types
        ReactiveAdapter adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(CompletableFuture.class);
        Publisher<Object> publisher = adapter.toPublisher(CompletableFuture.completedFuture("from a future"));
        StepVerifier.create(Mono.from(publisher)).expectNext("from a future").verifyComplete();
        assertThat(adapter.getDescriptor().isMultiValue()).isFalse();
        // --8<-- [end:adapterRegistry]
    }

    @Test
    void scannableIntrospection() {
        // --8<-- [start:scannable]
        Flux<Integer> pipeline = Flux.range(1, 10).name("numbers").filter(i -> i % 2 == 0).publishOn(
                reactor.core.scheduler.Schedulers.parallel(), 32);
        Scannable s = Scannable.from(pipeline);
        assertThat(s.scan(Scannable.Attr.PREFETCH)).isEqualTo(32);       // publishOn's queue size
        assertThat(s.parents().map(Scannable::name).toList()).contains("numbers");
        // --8<-- [end:scannable]
    }

    @Test
    void fusionMakesSyncChainsCheap() {
        // --8<-- [start:fusion]
        // range → map → filter are "fuseable": Reactor collapses them into one loop with no queues,
        // so a long synchronous chain costs roughly what a hand-written for-loop costs.
        StepVerifier.create(Flux.range(1, 5).map(i -> i * 2).filter(i -> i > 4))
                .expectFusion()                                    // assert the chain negotiated fusion
                .expectNext(6, 8, 10)
                .verifyComplete();
        // --8<-- [end:fusion]
    }
}
