package com.example.reactive.playbook.codinginterview;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;
import reactor.core.publisher.Mono;

/** Problem 12: a reactive circuit breaker as a reusable operator: CLOSED -> OPEN after N failures -> HALF_OPEN after a cool-down. */
public class P12CircuitBreaker {

    public enum State { CLOSED, OPEN, HALF_OPEN }
    public static final class CallNotPermitted extends RuntimeException { CallNotPermitted() { super("circuit open"); } }

    private final int failureThreshold;
    private final Duration openFor;
    private final Clock clock;
    private State state = State.CLOSED;
    private int consecutiveFailures;
    private Instant openedAt;

    public P12CircuitBreaker(int failureThreshold, Duration openFor, Clock clock) {
        this.failureThreshold = failureThreshold; this.openFor = openFor; this.clock = clock;
    }

    /** Use with transformDeferred so the permission check happens per SUBSCRIPTION, not once at assembly. */
    public <T> Function<Mono<T>, Mono<T>> protect() {
        return source -> Mono.defer(() -> {
            if (!tryAcquire()) return Mono.error(new CallNotPermitted());
            return source.doOnSuccess(v -> onSuccess()).doOnError(e -> onFailure());
        });
    }

    private synchronized boolean tryAcquire() {
        if (state == State.OPEN && !clock.instant().isBefore(openedAt.plus(openFor))) state = State.HALF_OPEN;   // allow a trial
        return state != State.OPEN;
    }

    private synchronized void onSuccess() { consecutiveFailures = 0; state = State.CLOSED; }

    private synchronized void onFailure() {
        consecutiveFailures++;
        if (state == State.HALF_OPEN || consecutiveFailures >= failureThreshold) { state = State.OPEN; openedAt = clock.instant(); }
    }

    public synchronized State state() { return state; }
}
