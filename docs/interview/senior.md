# Senior (5–8 years)

??? question "1. Walk through how demand propagates in `Flux.range(1,1000).map(f).flatMap(g, 8).subscribe(...)`."
    `subscribe(lambda)` requests `Long.MAX_VALUE` from `flatMap`. `flatMap` (concurrency 8) requests **8**
    from `map`, which passes it to `range`. `range` emits 8 elements; `flatMap` subscribes to 8 inners,
    each inner requests its prefetch (32). When an inner completes, `flatMap` requests 1 more upstream
    (replenishment). So upstream sees bounded demand even though the final subscriber is unbounded —
    concurrency arguments are backpressure.

??? question "2. How do you design for a producer that can't be slowed down (e.g. market feed)?"
    Decide what may be lost: `onBackpressureLatest()` for state-like data (prices, positions);
    `onBackpressureDrop(metric++)` for telemetry; bounded `onBackpressureBuffer(n, DROP_OLDEST)` for recent
    history; `onBackpressureBuffer(n)` (errors on overflow) when nothing may be lost — then scale the
    consumer. Also `sample`/`bufferTimeout` to aggregate. Monitor drops. Never an unbounded buffer on an
    infinite source.

??? question "3. What is the Reactor Context and how does it differ from ThreadLocal?"
    An immutable map attached to the **subscription**, propagating from subscriber to upstream operators
    (`contextWrite` affects operators above it; the closest write wins). It survives thread hops, unlike
    `ThreadLocal`. Read with `deferContextual`/`transformDeferredContextual`. Spring Security, reactive
    transactions and Micrometer observation use it.

??? question "4. How do you get trace IDs into logs in a reactive app?"
    Use Micrometer Tracing + context-propagation with `spring.reactor.context-propagation=auto`
    (`Hooks.enableAutomaticContextPropagation()`): observation context stored in the Reactor Context is
    restored into ThreadLocals (MDC) around each operator. Logging pattern with `%X{traceId}`. WebClient
    and Kafka instrumentation propagate `traceparent` headers.

??? question "5. Explain cancellation and why it matters."
    `cancel()` travels upstream when a client disconnects, `take`/`timeout`/`switchMap`/`firstWithValue`
    are satisfied, or a `Disposable` is disposed. Well-behaved sources release resources (HTTP connections,
    DB cursors, Kafka subscriptions). It saves resources (e.g. stops an LLM generation, a DB query).
    Pitfalls: detached `subscribe()` doesn't receive the cancellation; side effects already triggered
    aren't rolled back unless you use `usingWhen`/transactions (which roll back on cancel).

??? question "6. How do you integrate Kafka in a reactive service now that reactor-kafka is discontinued?"
    Produce with `KafkaTemplate.send(...)` adapted via `Mono.fromFuture(() -> ...)`. Consume with
    `@KafkaListener` methods returning `Mono<Void>`: Spring Kafka (3.2+) treats them as async, releases the
    listener thread and commits offsets when the Mono completes (handling out-of-order completion).
    Errors go through `DefaultErrorHandler` (retries, DLT). Alternatives: Spring Cloud Stream reactive
    functions for stream transformations.

??? question "7. How do you keep a DB write and an event publish consistent?"
    The **transactional outbox**: insert the event into an `outbox` table in the same DB transaction, and
    relay it to Kafka asynchronously (Debezium CDC or a poller using `FOR UPDATE SKIP LOCKED`). Consumers
    must be idempotent because relays are at-least-once. Avoid "dual writes" and 2PC.

??? question "8. How do you make a Kafka consumer idempotent?"
    Natural idempotency (upserts, state-machine transitions that ignore stale events), unique constraints
    on business keys (`DuplicateKeyException` → treat as done), or a processed-events table keyed by event
    id inside the same transaction as the effect. Ordering per key via partitioning by aggregate id.

??? question "9. Describe circuit breaker states and configuration choices."
    CLOSED (calls pass, failures recorded in a sliding window) → OPEN when failure rate or slow-call rate
    exceeds a threshold (calls fail fast with `CallNotPermittedException`) → HALF_OPEN after a wait
    (limited trial calls) → CLOSED or OPEN. Choose window size/type (count vs time), minimum calls,
    thresholds, slow-call duration aligned with timeouts, and a fallback. In Reactor use
    `transformDeferred(CircuitBreakerOperator.of(cb))` so state is checked per subscription.

??? question "10. Bulkhead in reactive code?"
    Limit concurrency per dependency so one slow dependency can't consume everything: `flatMap(f, n)`,
    Resilience4j `BulkheadOperator`, separate connection pools per downstream (`ConnectionProvider`), and
    separate schedulers for blocking adapters.

??? question "11. How do you find blocking calls in a WebFlux code base?"
    BlockHound in tests/CI (fails on blocking calls on non-blocking threads), thread-dump inspection of
    `reactor-http-nio` threads under load, metrics on event-loop pending tasks, code review for JDBC,
    `RestTemplate`, `Files.*`, `.block()`, `synchronized`/locks, and libraries with hidden I/O
    (`UUID.randomUUID` on low-entropy systems, DNS lookups, logging appenders).

??? question "12. What are the main performance knobs of a WebFlux service?"
    Event-loop count (CPU requests), WebClient `ConnectionProvider` (max connections, pending acquire,
    idle time), R2DBC pool size/acquire timeout, codec max in-memory size, `flatMap` concurrency and
    prefetch, HTTP/2, async logging, JSON payload size, and GC/direct memory sizing. Measure p99 latency
    and pool pending acquisitions under realistic load.

??? question "13. How do you debug a pipeline that hangs?"
    Check demand (`.log()` shows `request(0)` or none), missing completion (a `Flux.create` that never calls
    `complete`, `Mono.never`, a `zip` with an empty source never completing other branches), `groupBy` with
    more groups than flatMap concurrency, a sink with no subscribers buffering, a `concatMap` blocked on a
    never-ending inner. Use `timeout` + `checkpoint` to locate; thread dumps if blocking.

??? question "14. `Flux.create` vs `Flux.generate` vs `Flux.push` — when each?"
    `generate`: synchronous, one emission per callback, state-driven, demand-aware automatically
    (iterators, cursors). `create`: multi-threaded, many emissions, bridge callbacks/listeners, choose an
    overflow strategy, use `onRequest` for demand. `push`: like `create` but single-threaded producer
    (slightly cheaper).

??? question "15. How would you fan out live events to thousands of SSE clients?"
    One upstream subscription (Kafka/Redis) per instance feeding a `Sinks.many().multicast()` (or
    `replay().limit(...)` for late joiners); each client gets `sink.asFlux().filter(...)` with
    `onBackpressureLatest()` and heartbeats; complete streams on terminal events; scale horizontally with
    every instance consuming all events (unique group id or pub/sub); use HTTP/2 to reduce connection cost.

??? question "16. How do you propagate the caller's JWT to downstream services?"
    An `ExchangeFilterFunction` that reads the token from `ReactiveSecurityContextHolder` and sets the
    `Authorization` header; or `ServerOAuth2AuthorizedClientExchangeFilterFunction` for client-credentials
    tokens; or Spring Cloud Gateway's `TokenRelay` filter at the edge. Consider token exchange for least
    privilege.

??? question "17. What is the risk of `@Transactional` methods that call WebClient?"
    The DB connection stays checked out while waiting on the network — pool exhaustion under latency; the
    remote call isn't part of the transaction (no rollback); retries may duplicate side effects. Do remote
    calls before/after the transaction and use outbox/sagas for cross-service consistency.

??? question "18. How do you test code that depends on Reactor Context?"
    `StepVerifier.create(publisher, StepVerifierOptions.create().withInitialContext(Context.of(...)))`,
    or `.contextWrite(...)` in the test; `expectAccessibleContext().contains(k, v)` to assert propagation.
    For security, `@WithMockUser`/`mockJwt()`.

??? question "19. What's wrong with `doOnNext(x -> repository.save(x))`?"
    `repository.save(x)` returns a `Mono` that is never subscribed — nothing is saved. Even with
    `.subscribe()` inside, it would be detached (no backpressure, errors lost, outside transactions,
    no cancellation). Use `flatMap(repository::save)` or `concatMap`.

??? question "20. How do you implement rate limiting for outbound calls?"
    Resilience4j `RateLimiterOperator`, pacing with `delayElements`/`zipWith(Flux.interval)`, a
    token-bucket (Bucket4j) check in `flatMap`, `limitRate` for demand shaping (not time-based). At the
    edge, Gateway `RequestRateLimiter` with Redis.

??? question "21. What happens with exceptions thrown inside `subscribe(onNext)` lambdas?"
    They're not routed to `onError` of the same subscriber (the sequence already delivered the element) —
    Reactor treats it as a bug: the subscription is cancelled and the error goes to
    `Hooks.onErrorDropped`/logged. Keep logic in operators; terminal lambdas should be trivial.

??? question "22. Explain `usingWhen` and where Spring uses it."
    Reactive try-with-resources with **async** acquire and separate async completion, error and
    cancellation handlers. Spring's reactive transaction support follows the same model (commit on
    complete, rollback on error/cancel), and R2DBC connection handling uses it to close connections.

??? question "23. How do you size `boundedElastic` and when is it a smell?"
    Default cap 10×cores threads and 100k queued tasks per worker. Growth in active/queued tasks means
    blocking work is piling up — the service is effectively thread-per-request again. Isolate legacy
    blocking adapters in dedicated schedulers with explicit sizes, or move the service to MVC + virtual
    threads if most work is blocking.

??? question "24. How do you handle partial failure when aggregating N services?"
    Per-call timeouts and fallbacks (`onErrorResume` to a degraded value), `Mono.zip` only for mandatory
    parts; optional parts via `.map(Optional::of).onErrorReturn(Optional.empty())` or `defaultIfEmpty`;
    `firstWithValue` for redundant sources; return partial responses with flags rather than failing the
    whole request; propagate deadlines (budget) downstream.

??? question "25. What metrics would you put on a reactive service dashboard?"
    RED per route (rate, errors, p95/p99 duration), WebClient client metrics per downstream, R2DBC pool
    acquired/pending, event-loop pending tasks, `boundedElastic` active/queued, Kafka consumer lag and
    listener latency, circuit-breaker state, JVM heap + direct memory + GC, SSE/WebSocket active
    connections.
