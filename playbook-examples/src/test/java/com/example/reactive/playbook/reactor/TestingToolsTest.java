package com.example.reactive.playbook.reactor;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.StepVerifierOptions;
import reactor.test.publisher.PublisherProbe;
import reactor.test.publisher.TestPublisher;
import reactor.util.context.Context;

class TestingToolsTest {

    @Test
    void stepVerifierEssentials() {
        // --8<-- [start:stepVerifier]
        StepVerifier.create(Flux.range(1, 5).map(i -> i * 10))
                .expectNext(10)
                .expectNextMatches(v -> v > 15)
                .assertNext(v -> org.assertj.core.api.Assertions.assertThat(v).isEqualTo(30))
                .expectNextCount(2)
                .expectComplete()
                .verify(Duration.ofSeconds(1));     // ALWAYS terminate with verify*() — or nothing runs
        // --8<-- [end:stepVerifier]
    }

    @Test
    void virtualTime() {
        // --8<-- [start:virtualTime]
        // a daily report that would take 3 days of wall-clock time to test
        StepVerifier.withVirtualTime(() -> Flux.interval(Duration.ofDays(1)).map(d -> "report-" + d).take(3))
                .expectSubscription()
                .expectNoEvent(Duration.ofDays(1))
                .expectNext("report-0")
                .thenAwait(Duration.ofDays(2))
                .expectNext("report-1", "report-2")
                .verifyComplete();
        // --8<-- [end:virtualTime]
    }

    @Test
    void testPublisherDrivesSignalsManually() {
        // --8<-- [start:testPublisher]
        TestPublisher<String> source = TestPublisher.create();
        Flux<String> upper = source.flux().map(String::toUpperCase).onErrorReturn("RECOVERED");

        StepVerifier.create(upper)
                .then(() -> source.emit("a", "b"))       // emit + complete
                .expectNext("A", "B")
                .verifyComplete();

        TestPublisher<String> failing = TestPublisher.create();
        StepVerifier.create(failing.flux().map(String::toUpperCase).onErrorReturn("RECOVERED"))
                .then(() -> failing.next("x").error(new RuntimeException()))
                .expectNext("X", "RECOVERED")
                .verifyComplete();
        // --8<-- [end:testPublisher]
    }

    @Test
    void publisherProbeChecksWhichBranchRan() {
        // --8<-- [start:probe]
        PublisherProbe<String> fallback = PublisherProbe.of(Mono.just("fallback"));
        Mono<String> result = Mono.<String>empty().switchIfEmpty(fallback.mono());

        StepVerifier.create(result).expectNext("fallback").verifyComplete();
        fallback.assertWasSubscribed();
        // --8<-- [end:probe]
    }

    @Test
    void contextInTests() {
        // --8<-- [start:testContext]
        Mono<String> user = Mono.deferContextual(ctx -> Mono.just(ctx.<String>get("user")));
        StepVerifier.create(user, StepVerifierOptions.create().withInitialContext(Context.of("user", "asha")))
                .expectNext("asha")
                .verifyComplete();
        // --8<-- [end:testContext]
    }

    @Test
    void debuggingWithCheckpointAndLog() {
        // --8<-- [start:debug]
        Flux<Integer> f = Flux.just(1, 2, 0)
                .map(i -> 10 / i)
                .checkpoint("after-division")    // adds this description to the error's stack trace
                .log("calc");                    // logs every signal: onSubscribe, request, onNext, onError
        StepVerifier.create(f)
                .expectNext(10, 5)
                .verifyErrorSatisfies(e -> org.assertj.core.api.Assertions.assertThat(e.getSuppressed()[0])
                        .hasMessageContaining("after-division"));
        // --8<-- [end:debug]
    }
}
