# What Does This Print?

Short puzzles that reveal whether someone *really* understands assembly vs subscription, laziness,
Context and error semantics. Every answer is asserted by
`playbook-examples/src/test/java/com/example/reactive/playbook/interview/TrickyQuestionsTest.java`
(`print` appends to a list).

## 1. Nothing is subscribed

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/interview/TrickyQuestionsTest.java:q1"
```

??? success "Answer"
    ```text
    end
    ```
    The pipeline is assembled but never subscribed, so `doOnNext` never runs.

## 2. Eager `Mono.just`

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/interview/TrickyQuestionsTest.java:q2"
```

??? success "Answer"
    ```text
    1
    1
    1
    ```
    `counter.incrementAndGet()` runs **once**, at assembly. `Mono.just` replays the captured value to every subscriber. Use `Mono.fromCallable` for per-subscription evaluation.

## 3. `switchIfEmpty` arguments are eager

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/interview/TrickyQuestionsTest.java:q3"
```

??? success "Answer"
    ```text
    fallback() called
    a
    ```
    `fallback()` is a normal method call evaluated while assembling — even though the source isn't empty. Use `switchIfEmpty(Mono.defer(this::fallback))`.

## 4. Errors terminate the sequence

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/interview/TrickyQuestionsTest.java:q4"
```

??? success "Answer"
    ```text
    -10
    -1
    ```
    `1` → `10/-1 = -10`; `2` → division by zero → `onErrorReturn(-1)` emits -1 and **completes**. Element `3` is never processed.

## 5. Which Context value wins?

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/interview/TrickyQuestionsTest.java:q5"
```

??? success "Answer"
    ```text
    inner
    ```
    `contextWrite` is visible to operators *above* it; the write closest to the reader shadows outer ones.

## 6. `zip` with an empty source

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/interview/TrickyQuestionsTest.java:q6"
```

??? success "Answer"
    ```text
    zip was empty
    ```
    `zip` needs a value from every source; if one completes empty, `zip` completes empty.

## 7. Demand and `take`

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/interview/TrickyQuestionsTest.java:q7"
```

??? success "Answer"
    ```text
    emit 1
    got 1
    emit 2
    got 2
    ```
    `take(2)` cancels upstream after the second element, so `3..5` are never emitted (elements flow one at a time through the chain).

## 8. `cache()`

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/interview/TrickyQuestionsTest.java:q8"
```

??? success "Answer"
    ```text
    computing
    v
    v
    ```
    The supplier runs once; the result is replayed to later subscribers.

## 9. Error in the middle of a concatenation

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/interview/TrickyQuestionsTest.java:q9"
```

??? success "Answer"
    ```text
    a
    b
    recovered
    ```
    The error terminates the concatenation before `c`; `onErrorResume` switches to the fallback.

## 10. `doOnSuccess` on an empty Mono

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/interview/TrickyQuestionsTest.java:q10"
```

??? success "Answer"
    ```text
    success with null
    ```
    `doOnSuccess` fires on completion **with or without** a value (null when empty); `doOnNext` only fires with a value. This is why `Mono<Void>` side effects with `doOnSuccess` always run.
