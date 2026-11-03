package com.example.reactive.playbook.codinginterview;

import java.time.Duration;
import java.util.function.Supplier;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/** Problem 8: start an async job, then poll its status with exponential backoff until DONE/FAILED, with an overall deadline. */
public class P08JobPoller {

    public enum Status { RUNNING, DONE, FAILED }

    public static final class StillRunning extends RuntimeException { StillRunning() { super(null, null, false, false); } }
    public static final class JobFailed extends RuntimeException { public JobFailed(String m) { super(m); } }

    public static Mono<String> awaitResult(Supplier<Mono<Status>> statusCall, Supplier<Mono<String>> resultCall, Duration firstDelay, Duration deadline) {
        return Mono.defer(statusCall)                                                    // re-subscribed on every retry
                .flatMap(s -> switch (s) {
                    case RUNNING -> Mono.<Status>error(new StillRunning());
                    case FAILED -> Mono.error(new JobFailed("job failed"));
                    case DONE -> Mono.just(s);
                })
                .retryWhen(Retry.backoff(Long.MAX_VALUE, firstDelay).maxBackoff(Duration.ofSeconds(8))
                        .filter(e -> e instanceof StillRunning))                         // only "not yet" is retried
                .timeout(deadline)                                                       // the overall budget
                .then(Mono.defer(resultCall));
    }
}
