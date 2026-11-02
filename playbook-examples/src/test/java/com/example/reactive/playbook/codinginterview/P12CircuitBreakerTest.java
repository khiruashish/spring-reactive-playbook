package com.example.reactive.playbook.codinginterview;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class P12CircuitBreakerTest {

    static final class MutableClock extends Clock {
        Instant now = Instant.parse("2025-01-01T00:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId z) { return this; }
        public Instant instant() { return now; }
    }

    final MutableClock clock = new MutableClock();
    final P12CircuitBreaker breaker = new P12CircuitBreaker(3, Duration.ofSeconds(10), clock);
    final AtomicInteger calls = new AtomicInteger();
    volatile boolean healthy = false;

    Mono<String> remote() {
        return Mono.defer(() -> { calls.incrementAndGet(); return healthy ? Mono.just("ok") : Mono.error(new IllegalStateException("500")); })
                .transformDeferred(breaker.protect());
    }

    @Test
    void opensAfterThreshold_andStopsCallingTheDependency() {
        for (int i = 0; i < 3; i++) StepVerifier.create(remote()).expectError(IllegalStateException.class).verify();
        assertThat(breaker.state()).isEqualTo(P12CircuitBreaker.State.OPEN);
        StepVerifier.create(remote()).expectError(P12CircuitBreaker.CallNotPermitted.class).verify();
        assertThat(calls.get()).isEqualTo(3);                             // the 4th call never reached the dependency
    }

    @Test
    void halfOpenTrial_success_closesTheCircuit() {
        for (int i = 0; i < 3; i++) remote().onErrorResume(e -> Mono.empty()).block();
        clock.now = clock.now.plusSeconds(10);
        healthy = true;
        StepVerifier.create(remote()).expectNext("ok").verifyComplete();
        assertThat(breaker.state()).isEqualTo(P12CircuitBreaker.State.CLOSED);
    }

    @Test
    void halfOpenTrial_failure_reopensImmediately() {
        for (int i = 0; i < 3; i++) remote().onErrorResume(e -> Mono.empty()).block();
        clock.now = clock.now.plusSeconds(11);
        StepVerifier.create(remote()).expectError(IllegalStateException.class).verify();
        assertThat(breaker.state()).isEqualTo(P12CircuitBreaker.State.OPEN);
        StepVerifier.create(remote()).expectError(P12CircuitBreaker.CallNotPermitted.class).verify();
    }

    @Test
    void successResetsTheFailureCount() {
        remote().onErrorResume(e -> Mono.empty()).block();
        remote().onErrorResume(e -> Mono.empty()).block();
        healthy = true;
        remote().block();
        healthy = false;
        remote().onErrorResume(e -> Mono.empty()).block();
        assertThat(breaker.state()).isEqualTo(P12CircuitBreaker.State.CLOSED);   // 2 + reset + 1 < 3
    }
}
