# Lead (8–12 years)

??? question "1. How would you decide between WebFlux and Spring MVC with virtual threads for a new service?"
    Start from workload and team: request/response CRUD with blocking libraries (JPA, JDBC, SDKs) and
    moderate fan-out → **MVC + virtual threads** (simpler code, debugging, hiring). Streaming (SSE/WS/NDJSON),
    heavy fan-out/aggregation, backpressure-sensitive pipelines, gateways/BFFs, or a fully non-blocking
    stack already in place → **WebFlux**. Validate with a spike and load test (p99, memory, CPU), and
    record an ADR. Mixed estates are fine; consistency within a service matters more than across services.

??? question "2. Explain operator fusion and why it matters for performance."
    Reactor optimises chains: **macro-fusion** at assembly (e.g. scalar `just` paths, `Mono.just().map()`
    collapsed), **micro-fusion** at subscription where operators negotiate `SYNC`/`ASYNC` modes and share
    a queue or pull directly (`QueueSubscription`) instead of each operator buffering and signalling.
    Result: long synchronous chains cost about as much as a loop, and async boundaries (`publishOn`,
    `flatMap`) are where queues and allocations happen. Insight for design: minimise unnecessary thread
    hops; benchmark with JMH rather than guessing.

??? question "3. How do you structure a reactive service so business logic stays testable?"
    Ports & adapters: a pure `domain` (value objects, state machines, pricing/validation rules — plain
    Java, no Reactor where possible), an `application` layer that orchestrates with `Mono`/`Flux` against
    port interfaces, `infrastructure` adapters (R2DBC, WebClient, Kafka) and thin `api` controllers. Unit
    test the domain without Reactor, use cases with in-memory port fakes + StepVerifier, adapters with
    Testcontainers. Enforce with ArchUnit.

??? question "4. What are your team's non-negotiable reactive coding rules?"
    No `block()`/`subscribe()` in request paths; every I/O method returns `Mono`/`Flux`; timeouts on every
    remote call; bounded concurrency; retries only for idempotent transient failures; no ThreadLocal —
    Context instead; explicit empty handling; BlockHound in CI; StepVerifier with virtual time; ProblemDetail
    errors; named small methods over giant chains; observability tags on outbound calls.

??? question "5. How would you migrate a large MVC service to WebFlux?"
    Only with a measured reason. Strangler approach: introduce WebClient first; move data access to R2DBC
    per bounded context (keep Flyway on JDBC); convert bottom-up (repository → service → controller);
    port security/filters; replace MockMvc with WebTestClient; dual-run behind the gateway with traffic
    splitting; load-test parity; decommission. Avoid half-migrated states with blocking JPA behind WebFlux.

??? question "6. How do you implement a saga in reactive microservices?"
    Choreography (services react to events: `OrderPlaced` → `PaymentCaptured` → `CourierAssigned`, with
    compensations like `RefundIssued`) or orchestration (a saga orchestrator with persisted state machine
    issuing commands and handling replies/timeouts). Each step idempotent, events via outbox, timeouts as
    events (scheduled checks), state machine guards against out-of-order messages. Reactive code helps
    orchestration with non-blocking waits, but state must be durable — never only in a `Mono` in memory.

??? question "7. How do you version event contracts?"
    Additive changes are backward compatible (new optional fields; tolerant readers). Breaking changes go
    to a new topic/version (`...v2`) with dual publishing during migration, or use a schema registry with
    compatibility rules (BACKWARD/FULL). Keep events in a shared contracts module without framework
    dependencies, and add consumer-driven contract tests.

??? question "8. What is your testing strategy for a reactive platform?"
    Pyramid: pure domain unit tests; StepVerifier tests for use cases with fakes and virtual time; slice
    tests (`@WebFluxTest`, `@DataR2dbcTest`); Testcontainers ITs for adapters with `@ServiceConnection`;
    contract tests between services; a few E2E flows through the gateway; BlockHound in ITs; load tests
    (k6/Gatling) in a pipeline stage; chaos tests (toxiproxy) for timeouts/retries.

??? question "9. How would you add order cancellation to the capstone safely?"
    Model `CANCELLED` in the state machine with allowed transitions (before `PICKED_UP`); `POST /orders/{id}/cancel`
    with idempotency; persist the transition and an `OrderCancelled` outbox event in one transaction;
    payment-service refunds idempotently; delivery-service consumes the event, disposes the trip
    `Disposable` and marks the delivery cancelled; late delivery events are ignored by the state machine;
    notify via notification-service. Handle the race "picked up vs cancelled" with conditional updates
    (`WHERE status IN (...)`).

??? question "10. How do you handle multi-tenancy in a reactive service?"
    Resolve tenant at the edge (JWT claim/host), put it in the Reactor Context via a `WebFilter`, read it in
    repositories (R2DBC: schema/database routing with `AbstractRoutingConnectionFactory` keyed from the
    Context; or tenant column with enforced filters), tag metrics/logs, and propagate via headers on
    WebClient/Kafka. Test isolation explicitly.

??? question "11. When is `Flux.create` with a custom bridge the wrong solution?"
    When a supported reactive client exists (don't wrap a blocking SDK in `create` and block inside it),
    when demand cannot be respected and data must not be lost (you'd be choosing buffer-and-OOM), or when
    the source needs lifecycle management better handled by a framework (Kafka, JMS). Prefer official
    reactive drivers, `fromCallable+boundedElastic` for blocking calls, or Sinks for event bridges.

??? question "12. How do you make long-running streams survive deployments?"
    Clients reconnect (SSE `EventSource` does automatically) and resume from an id (`Last-Event-ID` mapped
    to Kafka offset or a sequence); servers complete streams on shutdown so connections drain; state lives
    outside instances (Kafka/Redis); readiness probes stop new connections before shutdown; avoid sticky
    in-memory state.

??? question "13. How do you approach Reactor upgrade or Spring Boot major upgrade (e.g. 3 → 4)?"
    Read release notes/migration guides (Boot 4: modular auto-config, Jackson 3, test slices moved,
    property renames like `spring.mongodb.*`, Gateway properties under `server.webflux`); upgrade
    dependencies with the BOM; run the full test suite including BlockHound and contract tests; use
    OpenRewrite recipes; canary deploy with metric comparison; watch deprecations.

??? question "14. How do you prevent a slow downstream from cascading failure?"
    Timeouts tighter than upstream budgets, bulkheads (concurrency limits/pools per dependency), circuit
    breakers with fallbacks, load shedding (reject early when queues grow), retry budgets (not just
    counts), hedging only for idempotent reads, and backpressure from consumers to producers.

??? question "15. What would you look for in a reactive code review?"
    Hidden blocking, unreturned publishers, nested `subscribe`, unbounded `flatMap`/buffers, `cache()`
    without TTL on data that changes, `onErrorContinue`, retries of non-idempotent calls, missing timeouts,
    swallowed errors, ThreadLocal usage, `switchIfEmpty` eager arguments, giant lambdas, missing tests for
    error and empty paths, Context-dependent code without tests.

??? question "16. How do you onboard developers new to reactive?"
    A golden-path template (service skeleton with standards built in), this playbook's levels, pair
    programming on real tickets, a review checklist, katas (the coding exercises), "what does this print"
    sessions, and visible production dashboards so they see the effect of their code.

??? question "17. What's the trade-off of using Kotlin coroutines on top of WebFlux?"
    Pros: sequential-looking code, structured concurrency, easier debugging, same non-blocking runtime.
    Cons: two mental models in a mixed Java/Kotlin estate, context bridging (`ReactorContext`), some
    operators have no direct equivalent (use `Flow` operators or interop). Good choice for Kotlin teams.

??? question "18. How do you expose a reactive API to non-reactive consumers?"
    It's HTTP — consumers don't care. Design pagination for large results (or NDJSON streaming with clear
    docs), stable error contracts (ProblemDetail), timeouts and rate limits. For in-process libraries,
    offer both: reactive API plus a thin blocking facade documented for non-reactive contexts only.

??? question "19. How do you implement request-level deadlines across services?"
    Accept a deadline header at the edge (or compute from SLO), store it in the Context, apply
    `timeout(remainingBudget)` on outbound calls and propagate a reduced deadline header downstream;
    abort work when the budget is spent; services reject requests with insufficient remaining budget.

??? question "20. What anti-patterns have you seen in reactive production systems and how were they fixed?"
    Strong answers are concrete: e.g. "JDBC call behind WebFlux exhausted event loops under load → moved
    to boundedElastic as a stopgap then R2DBC"; "retry storm after a 503 spike → added jitter and a circuit
    breaker"; "OOM from `collectList` of an export → switched to streaming NDJSON"; "lost errors from
    fire-and-forget `subscribe()` → moved to outbox and returned publishers".
