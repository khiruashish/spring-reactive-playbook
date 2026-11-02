package com.example.reactive.playbook.reactor;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class CombiningOperatorsTest {

    record Profile(String name, int orders, String tier) {
    }

    @Test
    void zipRunsInParallelAndCombines() {
        // --8<-- [start:zip]
        // three independent calls, executed concurrently, combined when ALL have a value
        Mono<String> name = Mono.just("Asha").delayElement(Duration.ofMillis(30));
        Mono<Integer> orders = Mono.just(12).delayElement(Duration.ofMillis(20));
        Mono<String> tier = Mono.just("GOLD").delayElement(Duration.ofMillis(10));

        Mono<Profile> profile = Mono.zip(name, orders, tier)
                .map(t -> new Profile(t.getT1(), t.getT2(), t.getT3()));

        StepVerifier.create(profile).expectNext(new Profile("Asha", 12, "GOLD")).verifyComplete();
        // if ANY source is empty, zip completes empty; if any errors, zip errors
        // --8<-- [end:zip]
    }

    @Test
    void mergeVsConcat() {
        // --8<-- [start:mergeConcat]
        StepVerifier.withVirtualTime(() -> {
                    Flux<String> slow = Flux.just("s1", "s2").delayElements(Duration.ofMillis(30));
                    Flux<String> fast = Flux.just("f1", "f2").delayElements(Duration.ofMillis(20));
                    return Flux.merge(slow, fast);          // subscribe to both now; interleave by time
                })
                .thenAwait(Duration.ofSeconds(1))
                .expectNext("f1", "s1", "f2", "s2")
                .verifyComplete();

        StepVerifier.withVirtualTime(() -> {
                    Flux<String> slow = Flux.just("s1", "s2").delayElements(Duration.ofMillis(30));
                    Flux<String> fast = Flux.just("f1", "f2").delayElements(Duration.ofMillis(20));
                    return Flux.concat(slow, fast);         // subscribe to `fast` only after `slow` completes
                })
                .thenAwait(Duration.ofSeconds(1))
                .expectNext("s1", "s2", "f1", "f2")
                .verifyComplete();
        // --8<-- [end:mergeConcat]
    }

    @Test
    void combineLatest() {
        // --8<-- [start:combineLatest]
        // recompute whenever EITHER input changes (e.g. price × fx-rate)
        StepVerifier.withVirtualTime(() -> Flux.combineLatest(
                        Flux.just(100, 200).delayElements(Duration.ofMillis(10)),     // price at t=10, 20
                        Flux.just(2, 3).delayElements(Duration.ofMillis(15)),         // rate at t=15, 30
                        (price, rate) -> price * rate))
                .thenAwait(Duration.ofSeconds(1))
                .expectNext(200, 400, 600)   // t=15: 100×2, t=20: 200×2, t=30: 200×3
                .verifyComplete();
        // --8<-- [end:combineLatest]
    }

    @Test
    void firstWithValueRacesSources() {
        // --8<-- [start:first]
        StepVerifier.withVirtualTime(() -> Mono.firstWithValue(
                        Mono.just("primary").delayElement(Duration.ofMillis(50)),
                        Mono.just("replica").delayElement(Duration.ofMillis(10))))  // hedged request
                .thenAwait(Duration.ofSeconds(1))
                .expectNext("replica")                                              // fastest wins, other cancelled
                .verifyComplete();
        // --8<-- [end:first]
    }

    @Test
    void sequencingWithThen() {
        // --8<-- [start:then]
        StringBuilder log = new StringBuilder();
        Mono<Void> audit = Mono.fromRunnable(() -> log.append("audit;"));
        Mono<Void> save = Mono.fromRunnable(() -> log.append("save;"));

        Mono<String> pipeline = save
                .then(audit)                              // run after save completes, ignore its value
                .then(Mono.just("done"));                 // then switch to another publisher
        StepVerifier.create(pipeline).expectNext("done").verifyComplete();
        StepVerifier.create(Mono.when(save, audit)).verifyComplete();  // wait for both, no values
        StepVerifier.create(Flux.just(2, 3).startWith(1)).expectNext(1, 2, 3).verifyComplete();
        // --8<-- [end:then]
    }

    @Test
    void zipWithFluxPairsElements() {
        StepVerifier.create(Flux.just("a", "b", "c").zipWith(Flux.range(1, 2), (s, i) -> s + i))
                .expectNext("a1", "b2")   // stops at the shorter source
                .verifyComplete();
    }
}
