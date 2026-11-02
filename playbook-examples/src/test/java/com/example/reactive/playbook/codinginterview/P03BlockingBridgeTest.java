package com.example.reactive.playbook.codinginterview;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

class P03BlockingBridgeTest {

    final AtomicReference<String> ranOn = new AtomicReference<>();
    final AtomicInteger calls = new AtomicInteger();
    final P03BlockingBridge.LegacyCustomerClient legacy = id -> {
        calls.incrementAndGet();
        ranOn.set(Thread.currentThread().getName());
        try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return "customer " + id;
    };

    @Test
    void blockingCall_runsOnTheDedicatedPool_notTheCaller() {
        var pool = P03BlockingBridge.dedicatedPool();
        try {
            StepVerifier.create(new P03BlockingBridge(legacy, pool).customer("c1")).expectNext("customer c1").verifyComplete();
            assertThat(ranOn.get()).startsWith("legacy-crm-");
        } finally { pool.dispose(); }
    }

    @Test
    void fromCallable_isLazy_nothingRunsUntilSubscribe() {
        Mono<String> mono = new P03BlockingBridge(legacy, Schedulers.boundedElastic()).customer("c2");
        assertThat(calls.get()).isZero();
        mono.block();
        mono.block();
        assertThat(calls.get()).isEqualTo(2);                  // runs once PER subscription
    }

    @Test
    void monoJust_blocksTheCallerAtAssemblyTime() {
        new P03BlockingBridge(legacy, Schedulers.boundedElastic()).customerWrong("c3");   // never subscribed!
        assertThat(calls.get()).isEqualTo(1);
        assertThat(ranOn.get()).isEqualTo(Thread.currentThread().getName());              // ran on the caller's thread
    }

    @Test
    void manyConcurrentCalls_doNotSerialise() {
        var pool = P03BlockingBridge.dedicatedPool();
        try {
            long start = System.nanoTime();
            var bridge = new P03BlockingBridge(legacy, pool);
            StepVerifier.create(reactor.core.publisher.Flux.range(1, 10).flatMap(i -> bridge.customer("c" + i)).count())
                    .expectNext(10L).verifyComplete();
            assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(400);         // 10 x 50 ms in parallel, not 500 ms
        } finally { pool.dispose(); }
    }
}
