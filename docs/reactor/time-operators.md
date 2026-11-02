# Time Operators

Time-based operators schedule work on `Schedulers.parallel()` by default, so they are fully
non-blocking — and testable instantly with **virtual time**.

## Delaying

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/TimeOperatorsTest.java:delay"
```

| Operator | Delays |
|---|---|
| `delayElements(d)` | every element (and spaces them out) |
| `Mono.delayElement(d)` | the single value |
| `delaySubscription(d)` / `delaySubscription(publisher)` | the subscription itself — the source starts later |
| `delaySequence(d)` | shifts the whole sequence, keeping the gaps between elements |
| `Mono.delay(d)` | emits `0L` after `d` — a non-blocking `sleep` |

## Sampling and throttling

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/TimeOperatorsTest.java:sample"
```

| Operator | Keeps |
|---|---|
| `sample(d)` | the **latest** value per period |
| `sampleFirst(d)` | the **first** value, then ignores the rest of the period (click throttling) |
| `sampleTimeout(v -> Mono.delay(d))` | a value only if no newer value arrives within `d` (**debounce**, type-ahead) |

## Measuring

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/TimeOperatorsTest.java:elapsed"
```

`timed()` gives richer info (`elapsed()`, `elapsedSinceSubscription()`, `timestamp()`) per element.

## Polling and repeating

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/TimeOperatorsTest.java:repeat"
```

`repeat` resubscribes after **completion**; `retry` resubscribes after **error**. `repeatWhenEmpty`
keeps polling until a value arrives.

## Time windows

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/TimeOperatorsTest.java:windowTime"
```

## Timeouts

See [Error Handling → Timeouts](error-handling.md#timeouts). Remember: `timeout` **cancels** the
upstream when it fires, so make sure cancellation actually releases the resource.
