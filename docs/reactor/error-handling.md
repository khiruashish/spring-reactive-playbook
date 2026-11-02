# Error Handling

In Reactor an exception becomes an **`onError` signal** that travels downstream and **terminates** the
sequence. There is no `try/catch` around a pipeline — you place error operators *in* the pipeline.

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/ErrorHandlingTest.java:terminal"
```

## Recovery operators

| Operator | Equivalent imperative idea |
|---|---|
| `onErrorReturn(value)` / `onErrorReturn(Type.class, value)` | `catch → return default` |
| `onErrorResume(e -> publisher)` | `catch → call fallback` (cache, secondary service) |
| `onErrorMap(e -> newException)` | `catch → throw new DomainException(e)` |
| `onErrorComplete()` | `catch → ignore` |
| `doOnError(e -> log)` | `catch → log → rethrow` |
| `doFinally(signal -> ...)` | `finally` |

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/ErrorHandlingTest.java:fallbacks"
```

## Recover per element, not per stream

Put the recovery **inside** `flatMap` so one bad element doesn't terminate the whole stream:

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/ErrorHandlingTest.java:perElement"
```

!!! danger "Avoid `onErrorContinue`"
    `onErrorContinue` changes the behaviour of *upstream* operators in non-obvious ways and only works
    with operators that explicitly support it. The inner-`onErrorResume` pattern above is explicit and
    predictable.

## Retries

`retry(n)` resubscribes immediately — that hammers a struggling dependency. Use `retryWhen` with
**exponential backoff, jitter and a filter** for transient errors only:

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/ErrorHandlingTest.java:retry"
```

!!! warning "Only retry what is safe to retry"
    Retrying a `POST /payments` can charge twice unless the call is **idempotent** (e.g. carries an
    `Idempotency-Key`). The capstone's order-service does exactly that.

## Timeouts

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/ErrorHandlingTest.java:timeout"
```

Put `timeout` **before** `retryWhen` for a per-attempt limit, **after** it for an overall limit.

## Checked exceptions inside lambdas

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/ErrorHandlingTest.java:checked"
```

## Observing signals without changing them

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/ErrorHandlingTest.java:doOn"
```

## Errors you should not swallow

- `Exceptions.isOverflow(e)` — backpressure bug, fix the design.
- `Exceptions.isBubbling(e)` / `Error` subclasses — fatal, let them propagate.
- "Unhandled" errors from a bare `subscribe()` go to `Hooks.onErrorDropped` and are just logged —
  always pass an error consumer.
