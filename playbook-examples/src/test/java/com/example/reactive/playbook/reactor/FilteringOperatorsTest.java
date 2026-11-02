package com.example.reactive.playbook.reactor;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class FilteringOperatorsTest {

    @Test
    void filterFamily() {
        // --8<-- [start:filter]
        Flux<Integer> n = Flux.just(1, 2, 2, 3, 3, 3, 4, 1);
        StepVerifier.create(n.filter(i -> i % 2 == 0)).expectNext(2, 2, 4).verifyComplete();
        StepVerifier.create(n.distinct()).expectNext(1, 2, 3, 4).verifyComplete();
        StepVerifier.create(n.distinctUntilChanged()).expectNext(1, 2, 3, 4, 1).verifyComplete();
        StepVerifier.create(n.take(3)).expectNext(1, 2, 2).verifyComplete();          // then cancels upstream
        StepVerifier.create(n.skip(6)).expectNext(4, 1).verifyComplete();
        StepVerifier.create(n.takeWhile(i -> i < 3)).expectNext(1, 2, 2).verifyComplete();
        StepVerifier.create(n.skipUntil(i -> i == 4)).expectNext(4, 1).verifyComplete();
        StepVerifier.create(n.next()).expectNext(1).verifyComplete();                   // Flux → Mono (first)
        StepVerifier.create(n.last()).expectNext(1).verifyComplete();
        StepVerifier.create(n.elementAt(6)).expectNext(4).verifyComplete();
        // --8<-- [end:filter]
    }

    @Test
    void asyncPredicate() {
        // --8<-- [start:filterWhen]
        // keep only usernames that are NOT taken — the check itself is asynchronous
        Flux<String> candidates = Flux.just("ann", "bob", "cy");
        Flux<String> available = candidates.filterWhen(name -> isTaken(name).map(taken -> !taken));
        StepVerifier.create(available).expectNext("ann", "cy").verifyComplete();
        // --8<-- [end:filterWhen]
    }

    static Mono<Boolean> isTaken(String name) {
        return Mono.just(name.equals("bob"));
    }

    @Test
    void handlingEmpty() {
        // --8<-- [start:empty]
        Mono<String> missing = Mono.empty();
        StepVerifier.create(missing.defaultIfEmpty("guest")).expectNext("guest").verifyComplete();
        StepVerifier.create(missing.switchIfEmpty(Mono.fromSupplier(() -> "loaded lazily")))
                .expectNext("loaded lazily").verifyComplete();
        StepVerifier.create(missing.switchIfEmpty(Mono.error(new IllegalArgumentException("not found"))))
                .verifyError(IllegalArgumentException.class);
        StepVerifier.create(Flux.empty().hasElements()).expectNext(false).verifyComplete();
        // --8<-- [end:empty]
    }
}
