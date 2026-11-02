# Transforming

## `map` — synchronous 1:1

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/TransformingOperatorsTest.java:map"
```

Use `map` for pure, fast, non-blocking functions. If the function returns a `Mono`/`Flux`, you want
one of the `flatMap` family.

## The async family: `flatMap`, `concatMap`, `flatMapSequential`, `switchMap`

All four take `T -> Publisher<R>`. They differ in **concurrency** and **ordering**:

| Operator | Inner subscriptions | Output order | Use when |
|---|---|---|---|
| `flatMap` | concurrent (default 256) | completion order (interleaved) | independent async calls, max throughput |
| `concatMap` | one at a time | source order | order matters, or each step depends on the previous |
| `flatMapSequential` | concurrent | source order (buffers early results) | throughput **and** order |
| `switchMap` | only the latest; previous cancelled | latest only | type-ahead search, "latest value wins" |

Each example below looks up users with simulated latency (id 1 = 30 ms, 2 = 20 ms, 3 = 10 ms):

=== "flatMap"

    ```java
    --8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/TransformingOperatorsTest.java:flatMap"
    ```

=== "concatMap"

    ```java
    --8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/TransformingOperatorsTest.java:concatMap"
    ```

=== "flatMapSequential"

    ```java
    --8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/TransformingOperatorsTest.java:flatMapSequential"
    ```

=== "switchMap"

    ```java
    --8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/TransformingOperatorsTest.java:switchMap"
    ```

!!! warning "Bound your concurrency"
    `flatMap` subscribes to up to 256 inner publishers at once. Calling a downstream service for every
    element of a 10,000-element Flux can open hundreds of connections. Pass a concurrency argument:

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/TransformingOperatorsTest.java:flatMapConcurrency"
```

## `handle` — map + filter in one step

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/TransformingOperatorsTest.java:handle"
```

## Aggregation: `collectList`, `reduce`, `scan`, `count`, `collectMultimap`

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/TransformingOperatorsTest.java:aggregate"
```

!!! caution
    `collectList()` holds the whole stream in memory. Fine for a page of results, dangerous for an
    unbounded stream. Prefer streaming the `Flux` to the client.

## `expand` — recursive traversal

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/TransformingOperatorsTest.java:expand"
```

## `transform` — reuse operator chains

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/TransformingOperatorsTest.java:transform"
```

`transform` applies at assembly time; `transformDeferred` re-applies per subscriber (useful when the
chain depends on Context).
