package com.example.reactive.playbook.reactor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

class ContextAndResourcesTest {

    // --8<-- [start:context]
    /** Reads a value that was written further DOWN the chain (closer to the subscriber). */
    static Mono<String> greet(String name) {
        return Mono.deferContextual(ctx -> Mono.just(
                "[" + ctx.getOrDefault("tenant", "default") + "] hello " + name));
    }

    @Test
    void contextFlowsFromSubscriberUpwards() {
        Mono<String> result = greet("asha")
                .contextWrite(Context.of("tenant", "acme"));   // e.g. set by a WebFilter

        StepVerifier.create(result).expectNext("[acme] hello asha").verifyComplete();
    }
    // --8<-- [end:context]

    @Test
    void contextWriteOnlyAffectsOperatorsAboveIt() {
        // --8<-- [start:contextOrder]
        Mono<String> m = Mono.just("x")
                .flatMap(x -> Mono.deferContextual(ctx -> Mono.just(ctx.get("key").toString())))
                .contextWrite(Context.of("key", "inner"))   // visible to the flatMap above
                .contextWrite(Context.of("key", "outer"));  // shadowed: the closer write wins
        StepVerifier.create(m).expectNext("inner").verifyComplete();
        // --8<-- [end:contextOrder]
    }

    @Test
    void usingWhenManagesAsyncResources() {
        // --8<-- [start:usingWhen]
        List<String> log = new CopyOnWriteArrayList<>();
        Flux<String> rows = Flux.usingWhen(
                Mono.fromSupplier(() -> { log.add("open"); return "conn"; }),          // acquire
                conn -> Flux.just("row1", "row2"),                                        // use
                conn -> Mono.fromRunnable(() -> log.add("commit")),                       // on complete
                (conn, err) -> Mono.fromRunnable(() -> log.add("rollback")),              // on error
                conn -> Mono.fromRunnable(() -> log.add("rollback-cancel")));             // on cancel

        StepVerifier.create(rows).expectNext("row1", "row2").verifyComplete();
        assertThat(log).containsExactly("open", "commit");
        // --8<-- [end:usingWhen]
    }

    @Test
    void disposeCancelsTheSubscription() {
        // --8<-- [start:dispose]
        List<String> log = new CopyOnWriteArrayList<>();
        Disposable subscription = Flux.interval(java.time.Duration.ofMillis(5))
                .doOnCancel(() -> log.add("cancelled"))
                .subscribe();
        subscription.dispose();       // e.g. client disconnected → stop the upstream work
        assertThat(log).containsExactly("cancelled");
        assertThat(subscription.isDisposed()).isTrue();
        // --8<-- [end:dispose]
    }
}
