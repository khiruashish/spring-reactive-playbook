# Backpressure & Batching

## Demand, explicitly

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/BackpressureTest.java:request"
```

## `limitRate` — protect a source from unbounded requests

`block()`, `subscribe(lambda)` and many terminal consumers request `Long.MAX_VALUE`. `limitRate(n)`
re-shapes that into batches: request `n`, then replenish 75%.

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/BackpressureTest.java:limitRate"
```

## Overflow strategies

When the source **can't** slow down, pick what to lose:

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/BackpressureTest.java:overflow"
```

| Strategy | Keeps | Loses | Good for |
|---|---|---|---|
| `onBackpressureDrop()` | what fits current demand | everything else | metrics, telemetry |
| `onBackpressureLatest()` | the newest value | intermediate values | UI state, prices, positions |
| `onBackpressureBuffer(n, DROP_OLDEST)` | newest `n` | oldest | recent-history views |
| `onBackpressureBuffer(n)` | up to `n` | nothing — **errors** when full | must-not-lose, bounded |
| `onBackpressureBuffer()` | everything | memory 💥 | avoid on infinite sources |

## Batching: `buffer`, `bufferTimeout`, `window`, `groupBy`

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/BackpressureTest.java:batching"
```

!!! tip "`groupBy` needs enough concurrency"
    Each group is a `Flux` that must be consumed. With more distinct keys than `flatMap` concurrency
    (default 256), `groupBy` can hang. Keep cardinality low or raise concurrency.
