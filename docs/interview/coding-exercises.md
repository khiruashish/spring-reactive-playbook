# Coding Exercises (with tested solutions)

Try each exercise before expanding the solution. Every solution is a passing test in
`playbook-examples/src/test/java/com/example/reactive/playbook/interview/CodingExercisesTest.java`.

## 1. Concurrent dashboard (beginner)

Build a `Dashboard` from `users.apply(id)` (`Mono<User>`) and `orders.apply(id)` (`Flux<Order>`). The calls
are independent — total latency must be the **max**, not the sum.

??? success "Solution"
    ```java
    --8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/interview/CodingExercisesTest.java:ex1"
    ```

## 2. Bounded, ordered enrichment (intermediate)

Call a slow API for 100 ids with **at most 5 calls in flight**, returning results **in input order**.

??? success "Solution"
    ```java
    --8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/interview/CodingExercisesTest.java:ex2"
    ```

## 3. Selective retry (intermediate)

Retry only server-side failures (`IllegalStateException`, timeouts) 3 times with exponential backoff;
client errors fail immediately; after exhaustion surface the original exception.

??? success "Solution"
    ```java
    --8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/interview/CodingExercisesTest.java:ex3"
    ```

## 4. Single-flight cache (senior)

Concurrent callers for the same key must share **one** load; failures must not be cached.

??? success "Solution"
    ```java
    --8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/interview/CodingExercisesTest.java:ex4"
    ```

## 5. Batch insert (intermediate)

Insert a stream in batches of 100 **or** every 500 ms, whichever comes first, with at most 2 batches in
flight.

??? success "Solution"
    ```java
    --8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/interview/CodingExercisesTest.java:ex5"
    ```

## 6. Pagination as a stream (intermediate)

Turn a page-by-page API into one `Flux<String>` that stops at the first empty page.

??? success "Solution"
    ```java
    --8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/interview/CodingExercisesTest.java:ex6"
    ```

## 7. Per-key ordering (senior)

Process events **in order per account** but **in parallel across accounts**.

??? success "Solution"
    ```java
    --8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/interview/CodingExercisesTest.java:ex7"
    ```

## 8. Hedged request (senior)

Call the primary; if it hasn't answered in 50 ms, also call the replica; take the first value.

??? success "Solution"
    ```java
    --8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/interview/CodingExercisesTest.java:ex8"
    ```

## 9. Rate pacing (senior)

Emit at most N elements per second without dropping any.

??? success "Solution"
    ```java
    --8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/interview/CodingExercisesTest.java:ex9"
    ```

## 10. Find the bug (all levels)

The audit log is never written. Why, and how do you fix it?

??? success "Solution"
    ```java
    --8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/interview/CodingExercisesTest.java:ex10"
    ```

    `audit.apply(saved)` returns a `Mono` that nobody subscribes to. Side effects that are themselves
    reactive must be part of the chain (`flatMap`/`then`), never created inside `doOnNext`.
