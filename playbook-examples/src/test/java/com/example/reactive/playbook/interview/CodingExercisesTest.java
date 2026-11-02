package com.example.reactive.playbook.interview;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.retry.Retry;

/** Solutions to the interview coding exercises. Each is embedded in docs/interview/coding-exercises.md. */
class CodingExercisesTest {

    record User(String id, String name) { }
    record Order(String userId, long amountMinor) { }
    record Dashboard(User user, List<Order> orders, long totalMinor) { }

    // --8<-- [start:ex1]
    /** Ex 1: build a dashboard from two independent async calls, concurrently. */
    static Mono<Dashboard> dashboard(String userId,
                                     Function<String, Mono<User>> users,
                                     Function<String, Flux<Order>> orders) {
        return Mono.zip(users.apply(userId), orders.apply(userId).collectList())
                .map(t -> new Dashboard(t.getT1(), t.getT2(),
                        t.getT2().stream().mapToLong(Order::amountMinor).sum()));
    }
    // --8<-- [end:ex1]

    @Test
    void ex1() {
        StepVerifier.withVirtualTime(() -> dashboard("u1",
                        id -> Mono.just(new User(id, "Asha")).delayElement(Duration.ofSeconds(1)),
                        id -> Flux.just(new Order(id, 100), new Order(id, 250)).delayElements(Duration.ofMillis(400))))
                .thenAwait(Duration.ofSeconds(1))   // ≈ max(1s, 0.8s), not the 1.8s sum
                .assertNext(d -> assertThat(d.totalMinor()).isEqualTo(350))
                .verifyComplete();
    }

    // --8<-- [start:ex2]
    /** Ex 2: call a slow API for 100 ids, at most 5 concurrently, preserving input order. */
    static Flux<String> enrichAll(List<Integer> ids, Function<Integer, Mono<String>> api) {
        return Flux.fromIterable(ids).flatMapSequential(api, 5);
    }
    // --8<-- [end:ex2]

    @Test
    void ex2() {
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxInFlight = new AtomicInteger();
        Function<Integer, Mono<String>> api = id -> Mono.defer(() -> {
                    maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
                    return Mono.just("r" + id).delayElement(Duration.ofMillis(10 + (id % 3) * 5L));
                })
                .doOnTerminate(inFlight::decrementAndGet);   // before the slot is released downstream
        List<Integer> ids = java.util.stream.IntStream.rangeClosed(1, 100).boxed().toList();

        StepVerifier.create(enrichAll(ids, api).collectList())
                .assertNext(r -> assertThat(r).first().isEqualTo("r1"))
                .verifyComplete();
        assertThat(maxInFlight.get()).isLessThanOrEqualTo(5);
    }

    // --8<-- [start:ex3]
    /** Ex 3: retry only 5xx-like failures, 3 times, exponential backoff; give up with the original error. */
    static <T> Mono<T> resilient(Mono<T> call) {
        return call.timeout(Duration.ofSeconds(1))
                .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                        .filter(e -> e instanceof IllegalStateException || e instanceof java.util.concurrent.TimeoutException)
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
    }
    // --8<-- [end:ex3]

    @Test
    void ex3() {
        AtomicInteger attempts = new AtomicInteger();
        StepVerifier.withVirtualTime(() -> resilient(Mono.defer(() -> {
                    attempts.incrementAndGet();
                    return Mono.<String>error(new IllegalStateException("503"));
                })))
                .thenAwait(Duration.ofSeconds(5))
                .verifyErrorMessage("503");
        assertThat(attempts).hasValue(4);   // 1 call + 3 retries

        attempts.set(0);
        StepVerifier.create(resilient(Mono.defer(() -> {
                    attempts.incrementAndGet();
                    return Mono.<String>error(new IllegalArgumentException("400"));
                })))
                .verifyErrorMessage("400");
        assertThat(attempts).hasValue(1);   // client errors are not retried
    }

    // --8<-- [start:ex4]
    /** Ex 4: an in-memory cache where concurrent callers for the same key share ONE load. */
    static final class SingleFlightCache<K, V> {
        private final Map<K, Mono<V>> cache = new ConcurrentHashMap<>();
        private final Function<K, Mono<V>> loader;

        SingleFlightCache(Function<K, Mono<V>> loader) {
            this.loader = loader;
        }

        Mono<V> get(K key) {
            return cache.computeIfAbsent(key, k -> loader.apply(k)
                    .doOnError(e -> cache.remove(k))      // don't cache failures
                    .cache(Duration.ofMinutes(5)));       // share the in-flight/completed value
        }
    }
    // --8<-- [end:ex4]

    @Test
    void ex4() {
        AtomicInteger loads = new AtomicInteger();
        var cache = new SingleFlightCache<String, String>(k -> Mono.fromSupplier(() -> {
            loads.incrementAndGet();
            return k.toUpperCase();
        }).delayElement(Duration.ofMillis(20)));

        StepVerifier.create(Flux.merge(cache.get("a"), cache.get("a"), cache.get("a")))
                .expectNext("A", "A", "A")
                .verifyComplete();
        assertThat(loads).hasValue(1);
    }

    // --8<-- [start:ex5]
    /** Ex 5: batch-insert a stream: groups of 100 or every 500 ms, whichever first; max 2 batches in flight. */
    static Flux<Integer> batchInsert(Flux<String> rows, Function<List<String>, Mono<Integer>> insertBatch) {
        return rows.bufferTimeout(100, Duration.ofMillis(500))
                .flatMap(insertBatch, 2);
    }
    // --8<-- [end:ex5]

    @Test
    void ex5() {
        Flux<String> rows = Flux.range(1, 250).map(i -> "row" + i);
        StepVerifier.create(batchInsert(rows, batch -> Mono.just(batch.size())).collectList())
                .assertNext(sizes -> assertThat(sizes).containsExactlyInAnyOrder(100, 100, 50))
                .verifyComplete();
    }

    // --8<-- [start:ex6]
    /** Ex 6: paginate an API until an empty page, as one continuous Flux. */
    record Page(int number, List<String> items) { }

    static Flux<String> allItems(Function<Integer, Mono<Page>> fetchPage) {
        return fetchPage.apply(0)
                .expand(page -> page.items().isEmpty() ? Mono.empty() : fetchPage.apply(page.number() + 1))
                .concatMapIterable(Page::items);
    }
    // --8<-- [end:ex6]

    @Test
    void ex6() {
        Map<Integer, List<String>> pages = Map.of(0, List.of("a", "b"), 1, List.of("c"), 2, List.of());
        StepVerifier.create(allItems(n -> Mono.just(new Page(n, pages.getOrDefault(n, List.of())))))
                .expectNext("a", "b", "c")
                .verifyComplete();
    }

    // --8<-- [start:ex7]
    /** Ex 7: per-key ordering with cross-key parallelism (e.g. events for many accounts). */
    record Event(String account, int seq) { }

    static Flux<Event> processInOrderPerKey(Flux<Event> events, Function<Event, Mono<Event>> handler) {
        return events.groupBy(Event::account)
                .flatMap(group -> group.concatMap(handler));   // sequential within a key, parallel across keys
    }
    // --8<-- [end:ex7]

    @Test
    void ex7() {
        Flux<Event> events = Flux.just(new Event("A", 1), new Event("B", 1), new Event("A", 2), new Event("B", 2),
                new Event("A", 3));
        Function<Event, Mono<Event>> handler = e -> Mono.just(e).delayElement(Duration.ofMillis(e.account().equals("A") ? 15 : 5));
        StepVerifier.create(processInOrderPerKey(events, handler).filter(e -> e.account().equals("A")).map(Event::seq))
                .expectNext(1, 2, 3)
                .verifyComplete();
    }

    // --8<-- [start:ex8]
    /** Ex 8: hedged request — ask the replica only if the primary hasn't answered within 50 ms. */
    static <T> Mono<T> hedged(Mono<T> primary, Mono<T> replica) {
        return Mono.firstWithValue(primary, replica.delaySubscription(Duration.ofMillis(50)));
    }
    // --8<-- [end:ex8]

    @Test
    void ex8() {
        AtomicInteger replicaCalls = new AtomicInteger();
        Mono<String> replica = Mono.fromSupplier(() -> { replicaCalls.incrementAndGet(); return "replica"; });

        StepVerifier.withVirtualTime(() -> hedged(Mono.just("primary").delayElement(Duration.ofMillis(10)), replica))
                .thenAwait(Duration.ofSeconds(1)).expectNext("primary").verifyComplete();
        assertThat(replicaCalls).hasValue(0);   // fast primary → replica never called

        StepVerifier.withVirtualTime(() -> hedged(Mono.just("primary").delayElement(Duration.ofSeconds(2)), replica))
                .thenAwait(Duration.ofSeconds(1)).expectNext("replica").verifyComplete();
    }

    // --8<-- [start:ex9]
    /** Ex 9: rate-limit outgoing calls to N per second without dropping any. */
    static <T> Flux<T> ratePaced(Flux<T> source, int perSecond) {
        return source.buffer(perSecond)
                .zipWith(Flux.interval(Duration.ZERO, Duration.ofSeconds(1)), (batch, tick) -> batch)
                .concatMapIterable(Function.identity());
    }
    // --8<-- [end:ex9]

    @Test
    void ex9() {
        StepVerifier.withVirtualTime(() -> ratePaced(Flux.range(1, 7), 3))
                .expectSubscription()
                .expectNext(1, 2, 3)                       // t = 0
                .expectNoEvent(Duration.ofMillis(999))
                .thenAwait(Duration.ofMillis(1)).expectNext(4, 5, 6)   // t = 1s
                .thenAwait(Duration.ofSeconds(1)).expectNext(7)        // t = 2s
                .verifyComplete();
    }

    // --8<-- [start:ex10]
    /** Ex 10: fix the bug — the audit log was never written. */
    static Mono<String> saveBuggy(Function<String, Mono<String>> save, Function<String, Mono<Void>> audit, String v) {
        return save.apply(v).doOnNext(saved -> audit.apply(saved));     // ❌ Mono created, never subscribed
    }

    static Mono<String> saveFixed(Function<String, Mono<String>> save, Function<String, Mono<Void>> audit, String v) {
        return save.apply(v).flatMap(saved -> audit.apply(saved).thenReturn(saved)); // ✅ part of the chain
    }
    // --8<-- [end:ex10]

    @Test
    void ex10() {
        List<String> auditLog = new java.util.concurrent.CopyOnWriteArrayList<>();
        Function<String, Mono<String>> save = Mono::just;
        Function<String, Mono<Void>> audit = s -> Mono.fromRunnable(() -> auditLog.add(s));

        saveBuggy(save, audit, "x").block();
        assertThat(auditLog).isEmpty();
        saveFixed(save, audit, "y").block();
        assertThat(auditLog).containsExactly("y");
    }
}
