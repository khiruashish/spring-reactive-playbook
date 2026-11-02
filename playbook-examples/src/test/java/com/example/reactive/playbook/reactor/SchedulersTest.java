package com.example.reactive.playbook.reactor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

class SchedulersTest {

    @Test
    void publishOnSwitchesDownstream() {
        // --8<-- [start:publishOn]
        List<String> threads = new CopyOnWriteArrayList<>();
        Flux.range(1, 2)
                .doOnNext(i -> threads.add("before:" + Thread.currentThread().getName()))
                .publishOn(Schedulers.parallel())       // everything BELOW runs on `parallel`
                .doOnNext(i -> threads.add("after:" + Thread.currentThread().getName()))
                .blockLast();

        assertThat(threads).filteredOn(t -> t.startsWith("before:")).noneMatch(t -> t.contains("parallel"));
        assertThat(threads).anyMatch(t -> t.startsWith("after:parallel-"));
        // --8<-- [end:publishOn]
    }

    @Test
    void subscribeOnSwitchesTheSource() {
        // --8<-- [start:subscribeOn]
        List<String> threads = new CopyOnWriteArrayList<>();
        Mono.fromCallable(() -> {
                    threads.add(Thread.currentThread().getName());   // the (blocking) source
                    return "data";
                })
                .subscribeOn(Schedulers.boundedElastic())            // position doesn't matter
                .map(String::toUpperCase)
                .block();

        assertThat(threads).singleElement().asString().startsWith("boundedElastic-");
        // --8<-- [end:subscribeOn]
    }

    // --8<-- [start:wrapBlocking]
    /** The canonical way to call a blocking API (JDBC, legacy SDK, file I/O) from reactive code. */
    static Mono<String> legacyLookup(String key) {
        return Mono.fromCallable(() -> BlockingLegacyClient.get(key))   // lazy: runs on subscribe
                .subscribeOn(Schedulers.boundedElastic());              // never on the event loop
    }
    // --8<-- [end:wrapBlocking]

    @Test
    void wrapBlockingCall() {
        StepVerifier.create(legacyLookup("k")).expectNext("value-of-k").verifyComplete();
    }

    @Test
    void virtualThreadScheduler() throws Exception {
        // --8<-- [start:virtual]
        // Java 21+: a scheduler backed by virtual threads — cheap to block on, unbounded in count
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Scheduler virtual = Schedulers.fromExecutorService(executor, "virtual");
            String thread = Mono.fromCallable(() -> Thread.currentThread().isVirtual() ? "virtual" : "platform")
                    .subscribeOn(virtual)
                    .block();
            assertThat(thread).isEqualTo("virtual");
        }
        // -Dreactor.schedulers.defaultBoundedElasticOnVirtualThreads=true makes boundedElastic() virtual too
        // --8<-- [end:virtual]
    }

    @Test
    void parallelFlux() {
        // --8<-- [start:parallel]
        // CPU-bound work split across 'rails' (one per core by default)
        Mono<Long> sumOfSquares = Flux.range(1, 1_000)
                .parallel()
                .runOn(Schedulers.parallel())
                .map(i -> (long) i * i)
                .reduce(Long::sum);
        StepVerifier.create(sumOfSquares).expectNext(333_833_500L).verifyComplete();
        // --8<-- [end:parallel]
    }

    static final class BlockingLegacyClient {
        static String get(String key) throws InterruptedException {
            Thread.sleep(5);   // pretend this is a blocking network call
            return "value-of-" + key;
        }
    }
}
