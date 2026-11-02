# Intermediate (2–5 years)

??? question "1. Compare `flatMap`, `concatMap`, `flatMapSequential` and `switchMap`."
    All map `T -> Publisher<R>`. **flatMap**: subscribes eagerly to inners (default concurrency 256),
    output in completion order — max throughput. **concatMap**: one inner at a time, source order —
    use when order matters or steps depend on each other. **flatMapSequential**: concurrent subscription
    but re-ordered output (buffers early finishers). **switchMap**: cancels the previous inner when a new
    element arrives — "latest wins" (type-ahead search).

    **Follow-up:** *How do you cap concurrency?* `flatMap(f, 8)`; `concatMap` has prefetch but no
    concurrency.

??? question "2. Explain `publishOn` vs `subscribeOn`."
    `publishOn(s)` moves execution of **downstream** operators to scheduler `s` — position matters, it can
    appear several times. `subscribeOn(s)` moves the **subscription** (and therefore the source's
    emission if it is synchronous) to `s` — position doesn't matter, the one closest to the source wins.
    Typical: `Mono.fromCallable(blocking).subscribeOn(boundedElastic())`; `publishOn(parallel())` before
    CPU-heavy steps.

??? question "3. Which scheduler for which work?"
    `parallel()` (cores-sized) for CPU work and timers; `boundedElastic()` (10×cores threads, 100k queued
    tasks, idle eviction) for unavoidable blocking I/O; `single()` for low-latency serial tasks;
    `immediate()` no switch; custom (`fromExecutorService`) e.g. virtual threads. Never run blocking code
    on `parallel` or event loops.

??? question "4. How do you implement retries correctly?"
    `retryWhen(Retry.backoff(maxAttempts, minBackoff).maxBackoff(..).jitter(0.5).filter(isTransient)
    .onRetryExhaustedThrow((s, sig) -> sig.failure()))`. Retry only **transient** errors (5xx, timeouts,
    connection resets), only **idempotent** operations (or with idempotency keys), with backoff + jitter
    to avoid synchronized retry storms, and with a bounded count. Put `timeout` before it for per-attempt
    limits.

??? question "5. Where does `timeout` go relative to `retryWhen`?"
    Before `retryWhen`: each attempt has its own deadline (`call.timeout(2s).retryWhen(...)`). After it:
    one overall deadline for all attempts. Often both: per-attempt *and* overall budget.

??? question "6. What is the difference between `onErrorResume` and `onErrorContinue`?"
    `onErrorResume` replaces the failed sequence with a fallback publisher — the original sequence is
    terminated. `onErrorContinue` tries to drop the failing element and keep going, but it works only
    with operators that support it and affects **upstream** operators in surprising ways. Prefer
    per-element `onErrorResume` inside `flatMap`.

??? question "7. How does WebClient differ from RestTemplate/RestClient?"
    WebClient is non-blocking (Reactor Netty), returns `Mono`/`Flux`, supports streaming bodies (SSE,
    NDJSON), backpressure, and integrates with Reactor operators for timeouts/retries. RestTemplate and
    RestClient are blocking (fine on virtual threads in MVC apps). In Boot 4, the auto-configured
    `WebClient.Builder` comes from the `spring-boot-webclient` module.

??? question "8. `retrieve()` vs `exchangeToMono()`?"
    `retrieve()` is the high-level API: 4xx/5xx become `WebClientResponseException` (customisable with
    `onStatus`), then `bodyToMono/Flux/toEntity`. `exchangeToMono(resp -> ...)` hands you the raw response
    to branch on status/headers — but you must consume or release the body. Avoid the deprecated
    `exchange()` that could leak connections.

??? question "9. How do you handle errors centrally in WebFlux?"
    For annotated controllers: `@RestControllerAdvice` with `@ExceptionHandler` methods returning
    `ProblemDetail` (RFC 9457). For functional endpoints: `RouterFunctions.route().onError(...)` or recover
    in handlers. For everything else (filters, routing errors): a `WebExceptionHandler` ordered before Boot's
    default. Enable `spring.webflux.problemdetails.enabled` for framework exceptions.

??? question "10. How do you validate request bodies in WebFlux?"
    `@Valid @RequestBody Mono<Dto>` (or `Dto`) in annotated controllers — failures signal
    `WebExchangeBindException` (400). For functional endpoints, inject `jakarta.validation.Validator` and
    validate in the handler, mapping violations to a 400 ProblemDetail.

??? question "11. What is R2DBC and how is it different from JPA?"
    R2DBC is a non-blocking SPI for SQL databases. Spring Data R2DBC provides repositories, mapping and
    `R2dbcEntityTemplate`/`DatabaseClient`, but it is **not an ORM**: no lazy loading, no relationships,
    no dirty checking, no first-level cache. You model aggregates explicitly and load children with extra
    queries. Schema migration still uses Flyway/Liquibase (via JDBC).

??? question "12. Why does `save()` sometimes not insert with R2DBC?"
    Spring Data decides insert vs update from `@Id`: null → INSERT, non-null → UPDATE. If you assign ids
    yourself (UUIDs), `save()` issues an UPDATE affecting 0 rows. Fixes: DB-generated ids, implement
    `Persistable#isNew`, a `@Version` field, or `R2dbcEntityTemplate.insert()`.

??? question "13. How do reactive transactions work?"
    `R2dbcTransactionManager` (a `ReactiveTransactionManager`) binds the connection/transaction to the
    **Reactor Context**. `@Transactional` on a method returning `Mono`/`Flux`, or `TransactionalOperator`
    (`.as(tx::transactional)`), commits on completion, rolls back on error **and cancellation**. The whole
    pipeline must be returned; detached `subscribe()`s run outside it.

??? question "14. How does Spring Security work in WebFlux?"
    A `WebFilter` (`WebFilterChainProxy`) applies a `SecurityWebFilterChain` built with
    `ServerHttpSecurity`. Authentication uses `ReactiveAuthenticationManager` / `ReactiveUserDetailsService`.
    The `SecurityContext` lives in the Reactor Context — read it with `ReactiveSecurityContextHolder`
    (the servlet `SecurityContextHolder` is empty). Method security via `@EnableReactiveMethodSecurity`.

??? question "15. How do you configure a JWT resource server?"
    Add the oauth2-resource-server starter, set `spring.security.oauth2.resourceserver.jwt.issuer-uri`
    (JWKS discovered and cached), configure `.oauth2ResourceServer(rs -> rs.jwt(...))`. Scopes become
    `SCOPE_x` authorities; map custom role claims with a `JwtAuthenticationConverter` wrapped in
    `ReactiveJwtAuthenticationConverterAdapter`.

??? question "16. What are Sinks and why were Processors deprecated?"
    `Sinks` are thread-safe, spec-compliant ways to emit into a reactive stream from imperative code:
    `Sinks.one()`, `Sinks.empty()`, `Sinks.many().unicast()/multicast()/replay()`. Processors exposed both
    `Subscriber` and `Publisher` APIs and were easy to misuse (calling `onNext` concurrently, violating the
    spec). Sinks return an `EmitResult` (`tryEmitNext`) or take an `EmitFailureHandler` (`emitNext`).

??? question "17. `cache()` vs `share()`?"
    `cache()` subscribes upstream once and **replays** all past signals (optionally for a TTL) to every
    current and future subscriber — memoization. `share()` multicasts a live stream while there is at
    least one subscriber; late subscribers miss earlier elements; when everyone leaves, upstream is
    cancelled and a new subscriber triggers a new subscription.

??? question "18. How do you implement Server-Sent Events, and what operational concerns exist?"
    Return `Flux<ServerSentEvent<T>>` (or `Flux<T>`) with `produces = TEXT_EVENT_STREAM`. Concerns:
    heartbeats so proxies don't close idle connections, completing the stream when no longer needed,
    a strategy for slow clients (`onBackpressureLatest`), authentication (EventSource can't set headers),
    and multi-instance fan-out (shared broker).

??? question "19. SSE vs WebSocket?"
    SSE: server→client only, plain HTTP, auto-reconnect with `Last-Event-ID`, text only, proxy/HTTP-2
    friendly. WebSocket: bidirectional, binary support, needs upgrade, you handle reconnection and
    protocol. Use SSE for feeds/notifications, WebSocket for interactive bidirectional apps.

??? question "20. How do you call a blocking legacy SDK from WebFlux?"
    `Mono.fromCallable(() -> sdk.call(x)).subscribeOn(Schedulers.boundedElastic())`, with a `timeout`, and
    bounded concurrency at the call site (`flatMap(..., n)`) so you don't exhaust the pool. Consider a
    dedicated scheduler (or virtual-thread scheduler) to isolate it.

??? question "21. What are HTTP interface clients?"
    Interfaces annotated with `@HttpExchange`/`@GetExchange`... whose implementation Spring generates
    (`HttpServiceProxyFactory` + `WebClientAdapter`, or `@ImportHttpServices` in Framework 7). With reactive
    return types the proxy is non-blocking. Gotcha: collection query params are sent repeated
    (`?ids=1&ids=2`).

??? question "22. How do you test time-based code without waiting?"
    `StepVerifier.withVirtualTime(() -> publisher)` replaces the default `parallel` timer with a virtual
    clock; `thenAwait(Duration)` advances it and `expectNoEvent(d)` asserts silence. The publisher must be
    created inside the supplier so it picks up the virtual scheduler.

??? question "23. How do you test a WebClient-based client without a server?"
    Inject a `WebClient` built with `.exchangeFunction(req -> Mono.just(ClientResponse.create(...)...))`,
    or use OkHttp `MockWebServer`/WireMock for full HTTP fidelity. Assert with `StepVerifier`.

??? question "24. What is `Mono.defer` used for in `switchIfEmpty`?"
    The argument of `switchIfEmpty(x())` is evaluated at assembly — `x()` runs even if the source has a
    value. `switchIfEmpty(Mono.defer(this::x))` constructs the fallback only when needed.

??? question "25. How do you stream a large result from R2DBC to the client?"
    Return the repository `Flux` directly from the controller with a streaming media type (NDJSON/SSE) or
    plain JSON array; WebFlux writes elements as they arrive and backpressure flows to the database cursor.
    Avoid `collectList()`; keep the connection checkout short; consider `limitRate` to batch fetches.
