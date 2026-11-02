package com.example.reactive.playbook.codinginterview;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class P09PartialResultsTest {

    static Mono<String> process(String id) {
        if (id.contains("bad")) return Mono.error(new IllegalArgumentException("invalid row " + id));
        return Mono.delay(Duration.ofMillis(id.length() * 3L)).thenReturn(id.toUpperCase());
    }

    @Test
    void badRowsAreReported_goodRowsStillSucceed() {
        Flux<String> ids = Flux.just("a1", "bad-2", "a3", "bad-4", "a5");
        StepVerifier.create(P09PartialResults.importAll(ids, P09PartialResultsTest::process, 4))
                .assertNext(r -> {
                    assertThat(r.ok()).extracting(P09PartialResults.Ok::value).containsExactly("A1", "A3", "A5");
                    assertThat(r.failed()).extracting(P09PartialResults.Failed::reason).containsExactly("invalid row bad-2", "invalid row bad-4");
                }).verifyComplete();
    }

    @Test
    void withoutPerElementHandling_oneBadRowKillsEverything() {
        Flux<String> ids = Flux.just("a1", "bad-2", "a3");
        StepVerifier.create(ids.flatMap(P09PartialResultsTest::process).collectList())
                .expectErrorMessage("invalid row bad-2").verify();
    }

    @Test
    void largeImport_keepsInputOrder_withConcurrency() {
        Flux<String> ids = Flux.range(1, 200).map(i -> i % 50 == 0 ? "bad-" + i : "row-" + i);
        StepVerifier.create(P09PartialResults.importAll(ids, P09PartialResultsTest::process, 16))
                .assertNext(r -> {
                    assertThat(r.ok()).hasSize(196);
                    assertThat(r.failed()).hasSize(4);
                    assertThat(r.ok().get(0).id()).isEqualTo("row-1");
                }).verifyComplete();
    }
}
