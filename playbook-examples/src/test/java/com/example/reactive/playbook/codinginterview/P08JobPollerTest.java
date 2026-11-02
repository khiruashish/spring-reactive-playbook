package com.example.reactive.playbook.codinginterview;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class P08JobPollerTest {

    static java.util.function.Supplier<Mono<P08JobPoller.Status>> statuses(AtomicInteger polls, P08JobPoller.Status... seq) {
        Iterator<P08JobPoller.Status> it = List.of(seq).iterator();
        return () -> Mono.fromSupplier(() -> { polls.incrementAndGet(); return it.hasNext() ? it.next() : seq[seq.length - 1]; });
    }

    @Test
    void pollsWithBackoff_untilDone_thenFetchesResult() {
        AtomicInteger polls = new AtomicInteger();
        StepVerifier.withVirtualTime(() -> P08JobPoller.awaitResult(
                        statuses(polls, P08JobPoller.Status.RUNNING, P08JobPoller.Status.RUNNING, P08JobPoller.Status.RUNNING, P08JobPoller.Status.DONE),
                        () -> Mono.just("report.csv"), Duration.ofSeconds(1), Duration.ofMinutes(5)))
                .thenAwait(Duration.ofMinutes(1))
                .expectNext("report.csv").verifyComplete();
        assertThat(polls.get()).isEqualTo(4);
    }

    @Test
    void jobFailure_stopsPollingImmediately() {
        AtomicInteger polls = new AtomicInteger();
        StepVerifier.withVirtualTime(() -> P08JobPoller.awaitResult(statuses(polls, P08JobPoller.Status.RUNNING, P08JobPoller.Status.FAILED),
                        () -> Mono.just("never"), Duration.ofSeconds(1), Duration.ofMinutes(5)))
                .thenAwait(Duration.ofMinutes(1))
                .expectError(P08JobPoller.JobFailed.class).verify();
        assertThat(polls.get()).isEqualTo(2);
    }

    @Test
    void neverFinishes_timesOutAtTheDeadline() {
        AtomicInteger polls = new AtomicInteger();
        StepVerifier.withVirtualTime(() -> P08JobPoller.awaitResult(statuses(polls, P08JobPoller.Status.RUNNING),
                        () -> Mono.just("never"), Duration.ofSeconds(1), Duration.ofSeconds(30)))
                .thenAwait(Duration.ofSeconds(31))
                .expectError(TimeoutException.class).verify();
        assertThat(polls.get()).isBetween(3, 12);                // backoff, not a tight loop
    }
}
