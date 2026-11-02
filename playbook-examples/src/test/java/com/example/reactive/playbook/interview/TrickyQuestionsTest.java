package com.example.reactive.playbook.interview;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/** Each puzzle records what it "prints" into a list, and the test asserts the answer. */
class TrickyQuestionsTest {

    final List<String> out = new CopyOnWriteArrayList<>();

    void print(Object o) {
        out.add(String.valueOf(o));
    }

    @Test
    void q1() {
        // --8<-- [start:q1]
        Flux.just(1, 2, 3).map(i -> i * 10).doOnNext(this::print);
        print("end");
        // --8<-- [end:q1]
        assertThat(out).containsExactly("end");
    }

    @Test
    void q2() {
        // --8<-- [start:q2]
        AtomicInteger counter = new AtomicInteger();
        Mono<Integer> m = Mono.just(counter.incrementAndGet());
        m.subscribe(this::print);
        m.subscribe(this::print);
        print(counter.get());
        // --8<-- [end:q2]
        assertThat(out).containsExactly("1", "1", "1");
    }

    @Test
    void q3() {
        // --8<-- [start:q3]
        Mono.just("a")
                .switchIfEmpty(fallback())
                .subscribe(this::print);
        // --8<-- [end:q3]
        assertThat(out).containsExactly("fallback() called", "a");
    }

    Mono<String> fallback() {
        print("fallback() called");
        return Mono.just("b");
    }

    @Test
    void q4() {
        // --8<-- [start:q4]
        Flux.just(1, 2, 3)
                .map(i -> 10 / (i - 2))
                .onErrorReturn(-1)
                .subscribe(this::print);
        // --8<-- [end:q4]
        assertThat(out).containsExactly("-10", "-1");
    }

    @Test
    void q5() {
        // --8<-- [start:q5]
        Mono.deferContextual(ctx -> Mono.just(ctx.getOrDefault("user", "nobody")))
                .contextWrite(Context.of("user", "inner"))
                .contextWrite(Context.of("user", "outer"))
                .subscribe(this::print);
        // --8<-- [end:q5]
        assertThat(out).containsExactly("inner");
    }

    @Test
    void q6() {
        // --8<-- [start:q6]
        Mono.zip(Mono.just("a"), Mono.<String>empty())
                .map(t -> t.getT1() + t.getT2())
                .defaultIfEmpty("zip was empty")
                .subscribe(this::print);
        // --8<-- [end:q6]
        assertThat(out).containsExactly("zip was empty");
    }

    @Test
    void q7() {
        // --8<-- [start:q7]
        Flux.range(1, 5)
                .doOnNext(i -> print("emit " + i))
                .take(2)
                .subscribe(i -> print("got " + i));
        // --8<-- [end:q7]
        assertThat(out).containsExactly("emit 1", "got 1", "emit 2", "got 2");
    }

    @Test
    void q8() {
        // --8<-- [start:q8]
        Mono<String> m = Mono.fromSupplier(() -> { print("computing"); return "v"; }).cache();
        m.subscribe(this::print);
        m.subscribe(this::print);
        // --8<-- [end:q8]
        assertThat(out).containsExactly("computing", "v", "v");
    }

    @Test
    void q9() {
        // --8<-- [start:q9]
        Flux.just("a", "b")
                .concatWith(Mono.error(new RuntimeException("boom")))
                .concatWith(Mono.just("c"))
                .onErrorResume(e -> Mono.just("recovered"))
                .subscribe(this::print);
        // --8<-- [end:q9]
        assertThat(out).containsExactly("a", "b", "recovered");
    }

    @Test
    void q10() {
        // --8<-- [start:q10]
        Mono.<String>empty()
                .map(s -> s.toUpperCase())
                .doOnSuccess(v -> print("success with " + v))
                .doOnNext(v -> print("next " + v))
                .subscribe();
        // --8<-- [end:q10]
        assertThat(out).containsExactly("success with null");
    }
}
