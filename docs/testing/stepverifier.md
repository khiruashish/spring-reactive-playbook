# StepVerifier

`StepVerifier` subscribes to a publisher and asserts the exact sequence of signals.

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/TestingToolsTest.java:stepVerifier"
```

!!! danger "Forgetting `verify()`"
    `StepVerifier.create(flux).expectNext(1)` alone does **nothing** — no subscription happens until a
    `verify*()` method is called. A test that "passes" without `verify` asserts nothing.

## Virtual time

Replace the clock for operators that use `Schedulers.parallel()` timers (`delay`, `interval`, `timeout`,
`delayElements`). The publisher **must be created inside the supplier**:

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/TestingToolsTest.java:virtualTime"
```

## `TestPublisher` — drive signals by hand

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/TestingToolsTest.java:testPublisher"
```

`TestPublisher.createNoncompliant(...)` lets you test how your operators cope with misbehaving sources.

## `PublisherProbe` — which branch ran?

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/TestingToolsTest.java:probe"
```

## Context

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/TestingToolsTest.java:testContext"
```

## Cheat sheet

| Method | Asserts |
|---|---|
| `expectNext(a, b)` / `expectNextCount(n)` | values / number of values |
| `expectNextMatches(pred)` / `assertNext(consumer)` | value with a predicate / AssertJ |
| `thenConsumeWhile(pred)` | skip matching values |
| `expectNoEvent(d)` | silence for a duration (virtual time) |
| `thenRequest(n)` / `StepVerifier.create(p, 0)` | manual demand |
| `thenCancel()` | cancel and verify cancellation path |
| `verifyComplete()` / `verifyError(Type)` / `verifyErrorMessage(m)` | terminal signal |
| `verify(Duration)` | fail if it takes too long (real time) |
| `expectAccessibleContext()` | inspect Context |
