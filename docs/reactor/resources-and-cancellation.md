# Resources & Cancellation

## Cancellation is a first-class signal

When a client disconnects, `take(n)` is satisfied, a `timeout` fires or `switchMap` moves on, Reactor
sends **`cancel`** upstream. Well-behaved sources stop work and release resources.

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/ContextAndResourcesTest.java:dispose"
```

## `using` and `usingWhen`

The reactive `try-with-resources`. `usingWhen` supports **asynchronous** acquire and cleanup with
separate commit/rollback/cancel paths — exactly how reactive transactions are implemented.

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/ContextAndResourcesTest.java:usingWhen"
```

## Cleanup hooks

| Hook | Runs on |
|---|---|
| `doFinally(signalType -> ...)` | complete, error **or** cancel — after the signal propagated |
| `doOnCancel(...)` | cancel only |
| `doOnTerminate(...)` / `doAfterTerminate(...)` | complete or error |
| `FluxSink.onDispose(...)` in `Flux.create` | any termination of the sink |

!!! example "Server-side effect of cancellation"
    In WebFlux, an SSE client closing its browser tab cancels the response `Flux`, which cancels the
    `Flux.interval`, the database tailable cursor, the Kafka-backed sink subscription… all the way up.
    Long-running work started with a detached `subscribe()` does **not** get that cancellation — track
    its `Disposable` and dispose it yourself (the capstone's delivery-service does this on shutdown).
