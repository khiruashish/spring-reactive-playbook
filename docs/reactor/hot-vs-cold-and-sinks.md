# Hot vs Cold & Sinks

| | Cold | Hot |
|---|---|---|
| Data produced | per subscriber, from the start | independently of subscribers |
| Late subscriber sees | everything | only what is emitted after it joined (unless replayed) |
| Examples | `Flux.range`, WebClient calls, repository queries | UI events, Kafka topic, `Sinks.many()`, `share()`d intervals |

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/HotColdAndSinksTest.java:cold"
```

## Turning cold into hot

| Operator | Behaviour |
|---|---|
| `cache()` / `cache(ttl)` | subscribe once, replay **all** (or within TTL) to everyone |
| `share()` | multicast while ≥ 1 subscriber; re-subscribe upstream when they come back |
| `publish().autoConnect(n)` | start when `n` subscribers arrived; never auto-disconnect |
| `publish().refCount(n, grace)` | start at `n`, stop after the last leaves (+ grace period) |
| `replay(n).autoConnect()` | like publish, but late subscribers get the last `n` |

=== "cache"

    ```java
    --8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/HotColdAndSinksTest.java:cache"
    ```

=== "share"

    ```java
    --8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/HotColdAndSinksTest.java:share"
    ```

=== "autoConnect"

    ```java
    --8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/HotColdAndSinksTest.java:autoConnect"
    ```

## Sinks — emitting from imperative code

`Sinks` are the safe, spec-compliant way to push values into a reactive stream from callbacks,
controllers, listeners or schedulers (they replace the deprecated `Processor`s).

| Factory | Subscribers | Late subscriber gets |
|---|---|---|
| `Sinks.one()` | many | the single value |
| `Sinks.empty()` | many | completion only |
| `Sinks.many().unicast()` | exactly one | buffered elements |
| `Sinks.many().multicast().onBackpressureBuffer()` | many | elements after it joined (buffers until the first subscriber) |
| `Sinks.many().multicast().directBestEffort()` | many | elements after it joined; slow subscribers miss items |
| `Sinks.many().replay().all()` / `limit(n)` / `limit(Duration)` | many | history |

=== "multicast"

    ```java
    --8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/HotColdAndSinksTest.java:sinkMulticast"
    ```

=== "replay"

    ```java
    --8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/HotColdAndSinksTest.java:sinkReplay"
    ```

=== "one"

    ```java
    --8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/HotColdAndSinksTest.java:sinkOne"
    ```

## Emission results are not exceptions

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/HotColdAndSinksTest.java:emitResult"
```

!!! warning "Concurrent emitters"
    Sinks reject *concurrent* `tryEmitNext` calls with `FAIL_NON_SERIALIZED`. If several threads emit,
    use `emitNext(value, Sinks.EmitFailureHandler.busyLooping(Duration.ofMillis(50)))`, or serialise
    the emitters.
