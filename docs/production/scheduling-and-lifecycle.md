# Scheduling, Startup & Shutdown

## `@Scheduled` with reactive methods

Since Spring Framework 6.1, `@Scheduled` methods may return a `Publisher`: Spring subscribes on every
trigger — no `subscribe()`/`block()` needed.

```java
@Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
Mono<Void> expireCarts() {
    return carts.findExpired(Instant.now())
            .flatMap(carts::delete, 8)
            .then();
}
```

With `fixedDelay`, the next run starts after the returned `Mono` **completes**. In a cluster, guard
jobs with a distributed lock (ShedLock supports R2DBC/Mongo/Redis) so only one instance runs them.

## Reactive streams as background jobs

For continuous processing (e.g. consuming a change stream), start the pipeline on
`ApplicationReadyEvent`, keep its `Disposable`, add `retryWhen` for resilience, and dispose it on
shutdown:

```java
@Component
class ChangeStreamProjector implements SmartLifecycle {
    private Disposable subscription;

    @Override public void start() {
        subscription = changes.listen()
                .concatMap(projection::apply)
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1)).maxBackoff(Duration.ofMinutes(1)))
                .subscribe();
    }
    @Override public void stop() { subscription.dispose(); }
    @Override public boolean isRunning() { return subscription != null && !subscription.isDisposed(); }
}
```

## Graceful shutdown

```yaml
server:
  shutdown: graceful                     # stop accepting, finish in-flight requests
spring:
  lifecycle:
    timeout-per-shutdown-phase: 25s     # < Kubernetes terminationGracePeriodSeconds
```

Long-lived SSE/WebSocket streams don't "finish" — complete them on shutdown (e.g. `takeUntilOther` a
shutdown signal) so clients reconnect to another instance.

## Startup checks

Use reactive health indicators and readiness probes; don't `block()` in `@PostConstruct` to warm caches —
run warm-up in an `ApplicationRunner` that returns once the `Mono` completes (blocking in a runner on the
main thread is acceptable at startup).
