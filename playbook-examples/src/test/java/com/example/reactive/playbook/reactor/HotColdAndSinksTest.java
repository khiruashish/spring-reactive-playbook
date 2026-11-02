package com.example.reactive.playbook.reactor;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

class HotColdAndSinksTest {

    @Test
    void coldPublishersReplayForEachSubscriber() {
        // --8<-- [start:cold]
        AtomicInteger subscriptions = new AtomicInteger();
        Flux<Integer> cold = Flux.range(1, 3).doOnSubscribe(s -> subscriptions.incrementAndGet());

        cold.subscribe();
        cold.subscribe();
        assertThat(subscriptions).hasValue(2);   // each subscriber gets its own execution (e.g. its own HTTP call)
        // --8<-- [end:cold]
    }

    @Test
    void cacheTurnsColdIntoReplayingHot() {
        // --8<-- [start:cache]
        AtomicInteger calls = new AtomicInteger();
        Mono<String> config = Mono.fromCallable(() -> "config#" + calls.incrementAndGet())
                .cache(Duration.ofMinutes(5));   // compute once, replay to everyone for 5 min

        assertThat(config.block()).isEqualTo("config#1");
        assertThat(config.block()).isEqualTo("config#1");
        assertThat(calls).hasValue(1);
        // --8<-- [end:cache]
    }

    @Test
    void shareMulticastsALiveSource() {
        // --8<-- [start:share]
        AtomicInteger subscriptions = new AtomicInteger();
        Flux<Long> ticks = Flux.interval(Duration.ofMillis(10))
                .doOnSubscribe(s -> subscriptions.incrementAndGet())
                .take(5)
                .share();                  // = publish().refCount(1): one upstream, many subscribers

        List<Long> a = new CopyOnWriteArrayList<>();
        List<Long> b = new CopyOnWriteArrayList<>();
        ticks.subscribe(a::add);
        ticks.subscribe(b::add);
        ticks.blockLast();

        assertThat(subscriptions).hasValue(1);   // upstream subscribed once
        // --8<-- [end:share]
    }

    @Test
    void publishAutoConnect() {
        // --8<-- [start:autoConnect]
        Flux<Integer> source = Flux.range(1, 3).publish().autoConnect(2); // start when 2 subscribers arrive
        List<Integer> seen = new CopyOnWriteArrayList<>();
        source.subscribe(seen::add);
        assertThat(seen).isEmpty();          // still waiting for the second subscriber
        source.subscribe(seen::add);
        assertThat(seen).containsExactly(1, 1, 2, 2, 3, 3);
        // --8<-- [end:autoConnect]
    }

    @Test
    void sinksManyMulticast() {
        // --8<-- [start:sinkMulticast]
        // Programmatic hot source: emit from imperative code (listeners, controllers, schedulers)
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        Flux<String> events = sink.asFlux();

        StepVerifier.create(events.take(2))
                .then(() -> sink.tryEmitNext("price-changed"))
                .expectNext("price-changed")
                .then(() -> sink.tryEmitNext("stock-low"))
                .expectNext("stock-low")
                .verifyComplete();
        // --8<-- [end:sinkMulticast]
    }

    @Test
    void sinksReplay() {
        // --8<-- [start:sinkReplay]
        Sinks.Many<Integer> sink = Sinks.many().replay().limit(2);  // late subscribers get the last 2
        sink.tryEmitNext(1);
        sink.tryEmitNext(2);
        sink.tryEmitNext(3);
        StepVerifier.create(sink.asFlux().take(2)).expectNext(2, 3).verifyComplete();
        // --8<-- [end:sinkReplay]
    }

    @Test
    void sinksOne() {
        // --8<-- [start:sinkOne]
        Sinks.One<String> reply = Sinks.one();   // a promise you complete later, e.g. from a callback
        Mono<String> response = reply.asMono();
        reply.tryEmitValue("pong");
        StepVerifier.create(response).expectNext("pong").verifyComplete();
        // --8<-- [end:sinkOne]
    }

    @Test
    void emitResultMustBeChecked() {
        // --8<-- [start:emitResult]
        Sinks.Many<String> unicast = Sinks.many().unicast().onBackpressureBuffer();
        assertThat(unicast.tryEmitNext("a")).isEqualTo(Sinks.EmitResult.OK);
        unicast.tryEmitComplete();
        assertThat(unicast.tryEmitNext("b")).isEqualTo(Sinks.EmitResult.FAIL_TERMINATED);  // not thrown!
        // --8<-- [end:emitResult]
    }
}
