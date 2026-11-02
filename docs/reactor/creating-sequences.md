# Creating Sequences

## Static factories

| Factory | Behaviour |
|---|---|
| `Mono.just(v)` / `Flux.just(a, b)` | emit known values (evaluated **eagerly**, at assembly) |
| `Mono.empty()` / `Flux.empty()` | complete immediately |
| `Mono.error(e)` / `Mono.error(() -> e)` | fail immediately (supplier form is lazy) |
| `Mono.never()` | never signals — useful in tests and timeouts |
| `Flux.range(start, count)` | integer sequence |
| `Flux.fromIterable` / `fromArray` / `fromStream` | from collections (`fromStream` is single-use!) |
| `Mono.justOrEmpty(optional)` | `Optional` → `Mono` |
| `Mono.fromCallable` / `fromSupplier` / `fromRunnable` | **lazy**, per subscription |
| `Mono.fromFuture(() -> cf)` | `CompletableFuture` → `Mono` (supplier form defers the call) |
| `Mono.defer(() -> mono)` / `Flux.defer` | build the publisher lazily, per subscriber |
| `Flux.interval(d)` / `Mono.delay(d)` | time-based (runs on `Schedulers.parallel()`) |

## Eager `just` vs lazy `defer`

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/CreatingSequencesTest.java:defer"
```

!!! tip
    `Mono.just(expensiveCall())` calls `expensiveCall()` **immediately**, even if nobody subscribes.
    Use `Mono.fromCallable(this::expensiveCall)` or `Mono.defer(...)` for anything with side effects.

## `generate`: synchronous, stateful, one element per round

Demand-aware by construction — Reactor calls your generator only when downstream requests more.

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/CreatingSequencesTest.java:generate"
```

## `create`: bridge callback/listener APIs

`Flux.create` gives you a `FluxSink` you can call from **any thread, any number of times**. Choose an
`OverflowStrategy` for when the source ignores demand (`BUFFER` default, `DROP`, `LATEST`, `ERROR`,
`IGNORE`). Register `onDispose`/`onCancel` to release the underlying resource.

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/CreatingSequencesTest.java:create"
```

| | `generate` | `create` | `push` |
|---|---|---|---|
| Threads | single, synchronous | any, concurrent | single producer thread |
| Emissions per call | exactly one | unlimited | unlimited |
| Respects demand | yes, automatically | only via `sink.requestedFromDownstream()` / `onRequest` | same as create |

## Time-based sources

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/CreatingSequencesTest.java:interval"
```
