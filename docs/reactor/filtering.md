# Filtering

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/FilteringOperatorsTest.java:filter"
```

| Operator | Notes |
|---|---|
| `filter(predicate)` | synchronous predicate |
| `filterWhen(t -> Mono<Boolean>)` | asynchronous predicate (e.g. a lookup) |
| `distinct()` / `distinctUntilChanged()` | `distinct` keeps a set of everything seen — unbounded on infinite streams! |
| `take(n)` / `takeWhile` / `takeUntil` / `take(Duration)` | cancels upstream once satisfied |
| `skip(n)` / `skipWhile` / `skipUntil` | |
| `next()` / `last()` / `elementAt(i)` / `single()` | `Flux` → `Mono` |
| `ignoreElements()` / `then()` | keep only the completion signal |

## Asynchronous predicates

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/FilteringOperatorsTest.java:filterWhen"
```

## Dealing with empty

An empty `Mono` is not an error — it is *absence*. Decide explicitly what absence means:

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/FilteringOperatorsTest.java:empty"
```

!!! tip "`switchIfEmpty` argument is evaluated eagerly"
    `switchIfEmpty(loadFromDb())` *calls* `loadFromDb()` at assembly time even when the source is not
    empty (the returned publisher just isn't subscribed). Wrap expensive construction in
    `Mono.defer(() -> loadFromDb())`.
