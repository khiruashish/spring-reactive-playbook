package com.example.reactive.playbook.reactor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.retry.Retry;

class ErrorHandlingTest {

    static final class DownstreamException extends RuntimeException {
        DownstreamException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    @Test
    void errorsTerminateTheSequence() {
        // --8<-- [start:terminal]
        Flux<Integer> f = Flux.just(1, 2, 0, 4).map(i -> 10 / i);
        StepVerifier.create(f)
                .expectNext(10, 5)
                .verifyError(ArithmeticException.class);   // 4 is never processed
        // --8<-- [end:terminal]
    }

    @Test
    void staticFallbacks() {
        // --8<-- [start:fallbacks]
        Mono<String> failing = Mono.error(new IOException("disk"));

        StepVerifier.create(failing.onErrorReturn("default")).expectNext("default").verifyComplete();
        StepVerifier.create(failing.onErrorReturn(IOException.class, "io-default"))
                .expectNext("io-default").verifyComplete();
        StepVerifier.create(failing.onErrorResume(e -> Mono.just("from cache")))
                .expectNext("from cache").verifyComplete();
        StepVerifier.create(failing.onErrorComplete()).verifyComplete();     // swallow → empty
        StepVerifier.create(failing.onErrorMap(e -> new DownstreamException("storage failed", e)))
                .verifyErrorMatches(e -> e instanceof DownstreamException && e.getCause() instanceof IOException);
        // --8<-- [end:fallbacks]
    }

    @Test
    void errorsInsideFlatMapCanBeHandledPerElement() {
        // --8<-- [start:perElement]
        // handle errors on the INNER publisher so one bad element doesn't kill the whole stream
        Flux<String> results = Flux.just("1", "two", "3")
                .flatMap(s -> Mono.fromCallable(() -> Integer.parseInt(s))
                        .map(i -> "ok:" + i)
                        .onErrorResume(NumberFormatException.class, e -> Mono.just("bad:" + s)));
        StepVerifier.create(results).expectNext("ok:1", "bad:two", "ok:3").verifyComplete();
        // --8<-- [end:perElement]
    }

    @Test
    void retryWithBackoff() {
        // --8<-- [start:retry]
        AtomicInteger attempts = new AtomicInteger();
        Mono<String> flaky = Mono.defer(() -> attempts.incrementAndGet() < 3
                ? Mono.error(new IOException("transient"))
                : Mono.just("success on attempt " + attempts.get()));

        StepVerifier.withVirtualTime(() -> flaky.retryWhen(Retry.backoff(5, Duration.ofMillis(100))
                        .maxBackoff(Duration.ofSeconds(2))
                        .jitter(0.5)
                        .filter(IOException.class::isInstance)          // only retry what is retryable
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure())))
                .thenAwait(Duration.ofSeconds(10))
                .expectNext("success on attempt 3")
                .verifyComplete();
        // --8<-- [end:retry]
    }

    @Test
    void timeoutWithFallback() {
        // --8<-- [start:timeout]
        StepVerifier.withVirtualTime(() -> Mono.just("slow").delayElement(Duration.ofSeconds(5))
                        .timeout(Duration.ofSeconds(1), Mono.just("fallback")))
                .thenAwait(Duration.ofSeconds(1))
                .expectNext("fallback")
                .verifyComplete();

        StepVerifier.withVirtualTime(() -> Mono.never().timeout(Duration.ofSeconds(1)))
                .thenAwait(Duration.ofSeconds(1))
                .verifyError(TimeoutException.class);
        // --8<-- [end:timeout]
    }

    @Test
    void checkedExceptionsInLambdas() {
        // --8<-- [start:checked]
        Flux<String> lines = Flux.just("a", "b").map(s -> {
            try {
                return readFile(s);
            } catch (IOException e) {
                throw Exceptions.propagate(e);   // wraps; Reactor unwraps it again for onError
            }
        });
        StepVerifier.create(lines).verifyErrorSatisfies(e -> assertThat(e).isInstanceOf(IOException.class));
        // --8<-- [end:checked]
    }

    static String readFile(String name) throws IOException {
        throw new IOException("cannot read " + name);
    }

    @Test
    void sideEffectsOnSignals() {
        // --8<-- [start:doOn]
        StringBuilder trace = new StringBuilder();
        Mono<Integer> m = Mono.just(1)
                .doOnSubscribe(s -> trace.append("subscribe;"))
                .doOnNext(v -> trace.append("next:").append(v).append(';'))
                .doOnError(e -> trace.append("error;"))
                .doOnSuccess(v -> trace.append("success;"))
                .doFinally(signal -> trace.append("finally:").append(signal));
        m.block();
        assertThat(trace).hasToString("subscribe;next:1;success;finally:onComplete");
        // --8<-- [end:doOn]
    }
}
