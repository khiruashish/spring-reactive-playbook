package com.example.reactive.playbook.codinginterview;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class P11SearchAsYouTypeTest {

    @Test
    void debounces_cancelsStaleSearch_andSkipsDuplicates() {
        List<String> started = new CopyOnWriteArrayList<>();
        AtomicInteger cancelled = new AtomicInteger();
        StepVerifier.withVirtualTime(() -> {
                    Flux<String> keys = Flux.concat(
                            Mono.just("j"),                                                  // t=0
                            Mono.delay(Duration.ofMillis(100)).thenReturn("ja"),             // t=100
                            Mono.delay(Duration.ofMillis(100)).thenReturn("jav"),            // t=200  -> searched at 500
                            Mono.delay(Duration.ofMillis(500)).thenReturn("java"),           // t=700  -> searched at 1000, cancels "jav"
                            Mono.delay(Duration.ofMillis(1100)).thenReturn("java "));        // t=1800 -> same query after strip: skipped
                    return P11SearchAsYouType.results(keys, q -> Mono.delay(Duration.ofMillis(600))
                            .thenReturn(List.of(q + " tutorial", q + " streams"))
                            .doOnSubscribe(s -> started.add(q))
                            .doOnCancel(cancelled::incrementAndGet), Duration.ofMillis(300));
                })
                .thenAwait(Duration.ofSeconds(5))
                .expectNext(List.of("java tutorial", "java streams"))
                .verifyComplete();
        assertThat(started).containsExactly("jav", "java");
        assertThat(cancelled.get()).isEqualTo(1);
    }

    @Test
    void shortQueries_neverHitTheBackend() {
        List<String> started = new CopyOnWriteArrayList<>();
        StepVerifier.withVirtualTime(() -> P11SearchAsYouType.results(Flux.just("x"), q -> { started.add(q); return Mono.just(List.of()); }, Duration.ofMillis(300)))
                .thenAwait(Duration.ofSeconds(1)).verifyComplete();
        assertThat(started).isEmpty();
    }
}
