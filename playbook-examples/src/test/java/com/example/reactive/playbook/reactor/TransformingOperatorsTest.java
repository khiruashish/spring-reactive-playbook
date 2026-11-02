package com.example.reactive.playbook.reactor;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class TransformingOperatorsTest {

    /** Simulated async lookup: user id → user name, with a per-id latency. */
    static Mono<String> fetchUser(int id) {
        return Mono.just("user-" + id).delayElement(Duration.ofMillis(40L - id * 10L));
    }

    @Test
    void mapIsSynchronousOneToOne() {
        // --8<-- [start:map]
        StepVerifier.create(Flux.just("a", "b").map(String::toUpperCase))
                .expectNext("A", "B")
                .verifyComplete();
        // --8<-- [end:map]
    }

    @Test
    void flatMapIsAsyncAndInterleaves() {
        // --8<-- [start:flatMap]
        // ids 1..3 have latencies 30ms, 20ms, 10ms: flatMap subscribes to all at once → completion order
        StepVerifier.withVirtualTime(() -> Flux.just(1, 2, 3).flatMap(TransformingOperatorsTest::fetchUser))
                .thenAwait(Duration.ofSeconds(1))
                .expectNext("user-3", "user-2", "user-1")
                .verifyComplete();
        // --8<-- [end:flatMap]
    }

    @Test
    void concatMapPreservesOrderOneAtATime() {
        // --8<-- [start:concatMap]
        StepVerifier.withVirtualTime(() -> Flux.just(1, 2, 3).concatMap(TransformingOperatorsTest::fetchUser))
                .thenAwait(Duration.ofSeconds(1))
                .expectNext("user-1", "user-2", "user-3")   // sequential: total ≈ 60ms
                .verifyComplete();
        // --8<-- [end:concatMap]
    }

    @Test
    void flatMapSequentialIsConcurrentButOrdered() {
        // --8<-- [start:flatMapSequential]
        StepVerifier.withVirtualTime(() -> Flux.just(1, 2, 3).flatMapSequential(TransformingOperatorsTest::fetchUser))
                .thenAwait(Duration.ofSeconds(1))
                .expectNext("user-1", "user-2", "user-3")   // concurrent: total ≈ 30ms, results re-ordered
                .verifyComplete();
        // --8<-- [end:flatMapSequential]
    }

    @Test
    void flatMapConcurrencyCanBeBounded() {
        // --8<-- [start:flatMapConcurrency]
        Flux<String> bounded = Flux.range(1, 100)
                .flatMap(TransformingOperatorsTest::callSlowService, 8); // at most 8 in flight
        StepVerifier.create(bounded.count()).expectNext(100L).verifyComplete();
        // --8<-- [end:flatMapConcurrency]
    }

    static Mono<String> callSlowService(int i) {
        return Mono.just("r" + i).delayElement(Duration.ofMillis(1));
    }

    @Test
    void switchMapCancelsThePreviousInner() {
        // --8<-- [start:switchMap]
        // type-ahead search: each new query cancels the in-flight search for the previous one
        Flux<String> queries = Flux.just("r", "re", "rea")
                .delayElements(Duration.ofMillis(100));          // a keystroke every 100ms
        Flux<String> results = queries.switchMap(q ->
                Mono.just("results for " + q).delayElement(Duration.ofMillis(250))); // search takes 250ms

        StepVerifier.create(results).expectNext("results for rea").verifyComplete();
        // (tests that depend on timing should use StepVerifier.withVirtualTime — see Testing chapter)
        // --8<-- [end:switchMap]
    }

    @Test
    void handleIsMapPlusFilter() {
        // --8<-- [start:handle]
        Flux<Integer> parsed = Flux.just("1", "x", "3").handle((s, sink) -> {
            try {
                sink.next(Integer.parseInt(s));
            } catch (NumberFormatException ignored) {
                // emit nothing → element dropped
            }
        });
        StepVerifier.create(parsed).expectNext(1, 3).verifyComplete();
        // --8<-- [end:handle]
    }

    @Test
    void aggregation() {
        // --8<-- [start:aggregate]
        Flux<Integer> numbers = Flux.range(1, 5);
        StepVerifier.create(numbers.collectList()).expectNext(List.of(1, 2, 3, 4, 5)).verifyComplete();
        StepVerifier.create(numbers.reduce(0, Integer::sum)).expectNext(15).verifyComplete();
        StepVerifier.create(numbers.scan(0, Integer::sum)).expectNext(0, 1, 3, 6, 10, 15).verifyComplete();
        StepVerifier.create(numbers.count()).expectNext(5L).verifyComplete();
        StepVerifier.create(Flux.just("apple", "avocado", "banana").collectMultimap(s -> s.charAt(0)))
                .expectNext(Map.of('a', List.of("apple", "avocado"), 'b', List.of("banana")))
                .verifyComplete();
        // --8<-- [end:aggregate]
    }

    @Test
    void expandWalksRecursiveStructures() {
        // --8<-- [start:expand]
        // breadth-first walk of a tree, e.g. following "next page" links or category children
        Map<String, List<String>> tree = Map.of("root", List.of("a", "b"), "a", List.of("a1"), "b", List.of());
        Flux<String> all = Mono.just("root")
                .expand(node -> Flux.fromIterable(tree.getOrDefault(node, List.of())));
        StepVerifier.create(all).expectNext("root", "a", "b", "a1").verifyComplete();
        // --8<-- [end:expand]
    }

    @Test
    void transformReusesOperatorChains() {
        // --8<-- [start:transform]
        java.util.function.Function<Flux<String>, Flux<String>> sanitize =
                f -> f.map(String::trim).filter(s -> !s.isEmpty()).map(String::toLowerCase);

        StepVerifier.create(Flux.just(" Foo ", "  ", "BAR").transform(sanitize))
                .expectNext("foo", "bar")
                .verifyComplete();
        // --8<-- [end:transform]
    }
}
