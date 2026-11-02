package com.example.reactive.playbook.codinginterview;

import java.time.Duration;
import java.util.function.Function;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/** Problem 3: call a BLOCKING legacy client (JDBC, SOAP SDK) from WebFlux without blocking the event loop. */
public class P03BlockingBridge {

    /** The legacy API: blocks the calling thread. */
    public interface LegacyCustomerClient { String loadCustomer(String id); }

    private final LegacyCustomerClient legacy;
    private final Scheduler blockingPool;

    public P03BlockingBridge(LegacyCustomerClient legacy, Scheduler blockingPool) { this.legacy = legacy; this.blockingPool = blockingPool; }

    /**
     * fromCallable = lazy (runs per subscription), subscribeOn = WHERE it runs (a bounded pool for blocking work),
     * timeout = never wait forever on legacy I/O.
     */
    public Mono<String> customer(String id) {
        return Mono.fromCallable(() -> legacy.loadCustomer(id))
                .subscribeOn(blockingPool)
                .timeout(Duration.ofSeconds(2));
    }

    /** WRONG (for contrast): Mono.just evaluates eagerly on the CALLER's thread, i.e. the event loop. */
    public Mono<String> customerWrong(String id) { return Mono.just(legacy.loadCustomer(id)); }

    /** A dedicated, bounded pool: a slow legacy system can't exhaust the shared boundedElastic for everyone. */
    public static Scheduler dedicatedPool() { return Schedulers.newBoundedElastic(10, 100, "legacy-crm"); }

    public static Function<String, String> threadName() { return s -> s + " on " + Thread.currentThread().getName().replaceAll("-\\d+$", "-N"); }
}
