# Beginner (0–2 years)

??? question "1. What is reactive programming?"
    A declarative style of programming with **asynchronous data streams**: you describe a pipeline of
    transformations over values that arrive over time, and the runtime pushes values through it as they
    become available. Its key properties are: non-blocking execution, composition via operators,
    errors as signals, and **backpressure** (the consumer controls the rate).

    **Follow-up:** *Is reactive faster?* Not per request — it scales better under I/O-bound concurrency
    because threads are never parked waiting.

??? question "2. What problem does Spring WebFlux solve compared to Spring MVC?"
    Spring MVC (servlet, thread-per-request) blocks a thread for each in-flight request; throughput is
    capped by pool size and memory. WebFlux runs on an event loop (Reactor Netty) with a few threads and
    never blocks, so it handles many more concurrent connections, streams (SSE/WebSocket) naturally and
    supports backpressure. It is valuable for I/O-heavy, high-concurrency or streaming workloads.

??? question "3. What is the difference between `Mono` and `Flux`?"
    Both are Reactive Streams `Publisher`s. `Mono<T>` emits **0 or 1** element then completes (or errors);
    `Flux<T>` emits **0..N** elements (possibly infinite). `Mono<Void>` signals only completion/error.
    Use `Mono` for single results (`findById`, a save, an HTTP response), `Flux` for collections and streams.

??? question "4. What are the four Reactive Streams interfaces?"
    `Publisher` (`subscribe`), `Subscriber` (`onSubscribe`, `onNext`, `onError`, `onComplete`),
    `Subscription` (`request(n)`, `cancel()`), and `Processor` (both subscriber and publisher). The signal
    order is `onSubscribe onNext* (onError | onComplete)?`, and a publisher may never emit more `onNext`
    than requested.

??? question "5. What does \"nothing happens until you subscribe\" mean?"
    Building a pipeline (`Flux.just(..).map(..)`) only **assembles** a description. Execution starts at
    **subscription**, and data flows only after `request(n)`. In WebFlux, the framework subscribes to the
    publisher you return. If you create a `Mono` and drop it, the work never runs — a classic bug
    (e.g. calling `repository.save(x)` without returning or chaining it).

??? question "6. `map` vs `flatMap`?"
    `map` applies a **synchronous** function `T -> R`, 1:1. `flatMap` applies `T -> Publisher<R>` and
    subscribes to the inner publishers, merging their results — use it whenever the function itself is
    asynchronous (DB call, HTTP call). Using `map` with an async function gives you `Mono<Mono<R>>` and the
    inner call never executes.

??? question "7. What is the difference between `Mono.just` and `Mono.fromCallable`/`Mono.defer`?"
    `Mono.just(x)` captures a value that has **already been computed** at assembly time.
    `fromCallable(() -> compute())` runs `compute()` **lazily on each subscription**; `defer(() -> mono)`
    builds the whole publisher lazily. `Mono.just(callDatabase())` calls the database even if no one
    subscribes — a bug.

??? question "8. How do you handle an empty `Mono`?"
    `defaultIfEmpty(value)` for a default, `switchIfEmpty(otherMono)` for a fallback publisher (wrap in
    `Mono.defer` if building it is expensive), or `switchIfEmpty(Mono.error(new NotFoundException()))` to
    turn absence into an error that an advice maps to 404. `hasElement()` tests for presence.

??? question "9. How are errors handled in Reactor?"
    An exception becomes an `onError` signal that **terminates** the sequence and travels downstream.
    Operators: `onErrorReturn` (static fallback), `onErrorResume` (fallback publisher), `onErrorMap`
    (translate), `onErrorComplete` (swallow), `doOnError` (side effect), `retry`/`retryWhen`. Without
    handling, the error reaches the subscriber (in WebFlux → an error response).

??? question "10. What happens if you call `block()` in a WebFlux controller?"
    It blocks the Netty event-loop thread until the result is available, stalling all other connections on
    that loop. Reactor detects it on Netty/parallel threads and throws `IllegalStateException: block() ...
    are blocking, which is not supported in thread reactor-http-nio-N`. `block()` is for tests, `main` and
    batch code only.

??? question "11. How do you combine the results of two independent calls?"
    `Mono.zip(a, b)` subscribes to both **concurrently** and emits a `Tuple2` once both have a value
    (empty if either is empty, error if either errors). `a.zipWith(b, combiner)` is the instance form.
    Chaining with `flatMap` would run them sequentially.

??? question "12. What is `collectList()` and when is it dangerous?"
    It turns a `Flux<T>` into a `Mono<List<T>>` holding every element in memory. Fine for bounded results
    (a page); dangerous for large/unbounded streams (OOM, no streaming to the client). Prefer returning the
    `Flux` directly.

??? question "13. What return types can a WebFlux controller method have?"
    `Mono<T>`, `Flux<T>`, `Mono<ResponseEntity<T>>`, `ResponseEntity<Mono<T>>`/`ResponseEntity<Flux<T>>`,
    `Mono<Void>`, plain values, `ServerSentEvent` streams, and any type known to `ReactiveAdapterRegistry`
    (e.g. `CompletableFuture`, Kotlin `Flow`, `suspend` functions).

??? question "14. How do you return 201 Created with a Location header reactively?"
    ```java
    return body.flatMap(repository::save)
            .map(saved -> ResponseEntity.created(URI.create("/items/" + saved.id())).body(saved));
    ```
    The return type is `Mono<ResponseEntity<Item>>`.

??? question "15. What is `StepVerifier`?"
    Reactor's test utility (`reactor-test`) that subscribes to a publisher and asserts the exact sequence
    of signals: `expectNext`, `assertNext`, `expectNextCount`, `expectError`, `verifyComplete`. It must end
    with a `verify*()` call — otherwise nothing is subscribed and the test asserts nothing.

??? question "16. How do you test an endpoint in WebFlux?"
    With `WebTestClient`: bind to a controller (`bindToController`), a router (`bindToRouterFunction`),
    a slice context (`@WebFluxTest`) or a running server (`@SpringBootTest(webEnvironment=RANDOM_PORT)`),
    then `.get().uri(...).exchange().expectStatus().isOk().expectBody()...`.

??? question "17. What server does WebFlux use by default and how many threads?"
    Reactor Netty. Event-loop worker threads default to the number of available CPU cores (minimum 4),
    named `reactor-http-nio-N` (or `-epoll-` on Linux with native transport).

??? question "18. What is a cold publisher?"
    One that starts producing **per subscriber**, from the beginning — e.g. `Flux.range`, a WebClient
    call, a repository query. Two subscriptions mean two executions (two HTTP calls). Hot publishers emit
    regardless of subscribers (e.g. a `Sinks.many()` or a shared ticker).

??? question "19. What does `then()` do?"
    It ignores all elements and emits only the completion (as `Mono<Void>`), or with `then(other)`
    continues with another publisher after completion. Used to sequence side effects:
    `save(x).then(audit(x)).then(Mono.just("ok"))`.

??? question "20. Can you use JPA/JDBC with WebFlux?"
    Not directly — they block. Options: switch to R2DBC (or reactive Mongo/Redis); or wrap calls in
    `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())` with a sized pool; or, if most of the
    service is blocking, use Spring MVC with virtual threads instead.

??? question "21. What is backpressure in one sentence?"
    The subscriber signals how many elements it can handle (`request(n)`), and the publisher must not send
    more — protecting slow consumers from fast producers.

??? question "22. Difference between `Flux.concat` and `Flux.merge`?"
    `concat` subscribes to sources **one after another** and preserves source order; `merge` subscribes to
    all **at once** and interleaves elements by arrival time.

??? question "23. How do you log what a reactive pipeline is doing?"
    `.log()` (logs every signal including requests/cancel and the thread), `doOnNext`/`doOnError` for
    targeted logging, and `checkpoint("name")` to label errors with the assembly location.

??? question "24. What is a `ServerSentEvent` stream?"
    A `Flux` returned with `produces = text/event-stream`; WebFlux writes each element as an SSE frame as
    soon as it is emitted. Browsers consume it with `EventSource`, which auto-reconnects.

??? question "25. Name three things that must never run on an event-loop thread."
    `Thread.sleep`, blocking I/O (JDBC, `RestTemplate`, `InputStream.read`, file I/O), and waiting on
    futures/locks (`future.get()`, `block()`, `synchronized` waits). Also avoid heavy CPU work (big
    regexes, crypto on large data) without offloading.
