# Master Prompt — Spring Reactive Playbook + Food Delivery Capstone

Copy everything inside the box below into an AI coding assistant (Claude Code, etc.) to regenerate or
extend this project. It encodes every decision taken while building it.

```text
ROLE
You are a senior Java/Spring engineer and technical writer. Build "spring-reactive-webapp": a complete,
self-contained learning playbook for reactive programming with Spring, published as an MkDocs site,
plus runnable, tested code behind every example.

STACK (pin exact versions; verify each one resolves on Maven Central before using it)
- Java 21 baseline (must also build and run unchanged on Java 25 LTS)
- Latest GA Spring Boot (4.1.x at time of writing → Spring Framework 7, Reactor 2025.x / reactor-core 3.8,
  Reactor Netty 1.3, Spring Security 7, Spring Data 2026.0, Spring Kafka 4, Jackson 3)
- Spring Cloud release train compatible with that Boot line (Gateway server-webflux, reactive
  Resilience4j circuit breaker)
- Gradle 9 Kotlin DSL multi-project build with a version catalog (gradle/libs.versions.toml)
- Docs: MkDocs + Material for MkDocs (navigation.tabs, sticky tabs, instant navigation, search
  suggest/highlight, code copy + annotations, light/dark/auto palette toggle, admonitions, content tabs,
  Mermaid diagrams via superfences, pymdownx.snippets with check_paths: true)

STRUCTURE — TWO PARTS, IN THIS ORDER
PART 1 — THE PLAYBOOK (all tabs except the last)
  Cover EVERY topic and subtopic below. Every example in Part 1 uses GENERIC domains (numbers, strings,
  users, products, prices, stock tickers, orders-as-abstract-ids…). DO NOT use the food delivery
  domain anywhere in Part 1.
  Each page: concept explanation → marble/sequence diagram (Mermaid) where it helps → runnable example
  → "when to use / pitfalls" admonitions → key takeaways.
  Examples for Reactor and WebFlux MUST live as passing JUnit 5 tests in a `playbook-examples` module
  and be embedded into the pages with snippet section markers (// --8<-- [start:name] … [end:name]),
  so the docs can never drift from working code. Use StepVerifier.withVirtualTime for anything
  time-based (no Thread.sleep, no flaky timing).

  Tab "Home": overview, getting started (build, test, serve docs), this master prompt, quick learning path.
  Tab "Roadmap": a 0 → 15+ years career roadmap in five levels — Foundation (0–2), Practitioner (2–5),
      Senior (5–8), Lead (8–12), Architect (12–15+). Per level: topics to master (linked to pages),
      hands-on exercises, a checkpoint, typical mistakes, and the matching interview set.
  Tab "Fundamentals": why reactive (thread-per-request limits, I/O-bound workloads), Reactive Manifesto,
      Reactive Streams spec (Publisher/Subscriber/Subscription/Processor, signals, rules), backpressure,
      reactive vs virtual threads (Loom) — honest decision guide.
  Tab "Project Reactor": Mono & Flux, assembly vs subscription time, cold by default; creating
      sequences (just/empty/error/defer/fromCallable/fromIterable/range/interval/generate/create);
      transforming (map/flatMap/concatMap/flatMapSequential/switchMap/handle/expand/transform,
      collect/reduce/scan); filtering (filter/filterWhen/distinct/take/skip/next/defaultIfEmpty/
      switchIfEmpty); combining (zip/merge/concat/combineLatest/firstWithValue/then/when/startWith);
      error handling (onErrorReturn/Resume/Map/Complete, per-element recovery, retry/retryWhen backoff,
      timeout, Exceptions.propagate, doOn* hooks); schedulers (publishOn vs subscribeOn,
      boundedElastic for blocking, parallel/ParallelFlux, virtual-thread scheduler); hot vs cold
      (share, publish/autoConnect, cache, replay) and Sinks (one, many unicast/multicast/replay,
      EmitResult, emit failure handlers); backpressure strategies (request(n), limitRate,
      onBackpressureDrop/Latest/Buffer + overflow strategies) and batching (buffer, bufferTimeout,
      window, groupBy); Context (contextWrite, deferContextual, propagation direction); resources &
      cancellation (using/usingWhen, Disposable, doFinally); time operators (delayElements,
      delaySubscription, sample/sampleFirst/sampleTimeout, elapsed/timed, repeat/repeatWhen polling,
      time windows); debugging (log, checkpoint, Hooks, ReactorDebugAgent); operator cheat sheet;
      advanced internals (assembly/subscription/execution, operator fusion, prefetch/queues, Scannable,
      custom operators via transform/transformDeferred, JDK Flow + RxJava/Mutiny/coroutines interop,
      ReactiveAdapterRegistry, reactor-pool/extra/tools).
  Tab "Spring WebFlux": architecture (Netty event loop, HttpHandler, DispatcherHandler, request
      lifecycle); project setup; annotated controllers; functional endpoints (RouterFunction,
      HandlerFunction, HandlerFilterFunction); error handling (ProblemDetail/RFC 9457,
      @RestControllerAdvice, WebExceptionHandler); WebFilter; WebClient (retrieve vs exchange,
      onStatus, timeouts, retries, connection pool, filters); HTTP interface clients (@HttpExchange);
      streaming (SSE, NDJSON); WebSocket; codecs, CORS, multipart, server configuration; files and
      DataBuffers (zero-copy downloads, streaming uploads, buffer release); GraphQL (Spring for GraphQL,
      @BatchMapping, subscriptions); Reactor Netty used directly (HttpClient/TcpServer, pools, wiretap).
  Tab "Reactive Data": R2DBC (repositories, DatabaseClient, R2dbcEntityTemplate, pooling, schema
      management), reactive MongoDB (repositories, template, tailable cursors, change streams),
      reactive Redis (templates, caching, pub/sub), reactive transactions (@Transactional,
      TransactionalOperator); other stores (Cassandra, Elasticsearch, Neo4j), reactive @Cacheable with
      Caffeine async mode, reactive sessions (WebSession + Spring Session).
  Tab "Security": SecurityWebFilterChain, ReactiveUserDetailsService, ReactiveSecurityContextHolder,
      OAuth2 resource server with JWT, OAuth2 client + WebClient token relay, @EnableReactiveMethodSecurity.
  Tab "Messaging": Kafka in reactive apps (KafkaTemplate → Mono.fromFuture, async @KafkaListener
      returning Mono, ordering, idempotency, outbox; note reactor-kafka status), RSocket (4 interaction
      models), Spring Cloud Gateway (routes, predicates, filters, global filters), Spring Cloud Stream
      reactive functions, streaming LLM responses with Spring AI ChatClient.stream() over SSE.
  Tab "Resilience & Observability": timeouts, retries with jitter, circuit breaker, bulkhead,
      rate limiting, fallbacks; Micrometer observation, tracing & automatic context propagation,
      logging/MDC in reactive code, Actuator.
  Tab "Testing": StepVerifier (expectations, virtual time, context, TestPublisher, PublisherProbe),
      WebTestClient (bindToController/RouterFunction/server), Boot test slices and Testcontainers ITs,
      BlockHound.
  Tab "Production": performance tuning (event loops, pools, memory, codecs limits), best practices &
      anti-patterns (block(), subscribe() inside operators, ThreadLocal, unbounded flatMap, etc.),
      migrating from Spring MVC, Kotlin coroutines interop, reactive @Scheduled and background-stream
      lifecycle, graceful shutdown, containers/Kubernetes sizing for Netty, GraalVM native images.
  Tab "Interview": 110+ questions with detailed model answers split by experience band (Beginner 0–2,
      Intermediate 2–5, Senior 5–8, Lead 8–12, Architect 12–15+), each as a collapsible Q&A with
      follow-ups; 10 coding exercises and 10 "what does this print?" puzzles whose solutions are
      passing tests; system-design scenarios (live tracking at scale, aggregating BFF, exactly-once
      payments, streaming exports, real-time notifications, rate-limited partner APIs).

PART 2 — THE CAPSTONE (exactly ONE final tab, AFTER all other tabs: "Food Delivery App")
  Only here, build a microservice food delivery platform that applies the topics from Part 1, with a
  page per service plus overview/architecture, run instructions, end-to-end flow (Mermaid sequence
  diagram), testing and production hardening. Services (each a Spring Boot WebFlux app):
  - api-gateway (8080): Spring Cloud Gateway, JWT resource server (dev HS256 token endpoint),
    X-User-Id relay from the security context, correlation-id GlobalFilter, retry on idempotent GETs,
    serves a small static UI (vanilla JS) that orders and tracks live via SSE + WebSocket.
  - restaurant-service (8081): functional endpoints, R2DBC PostgreSQL, programmatic validation,
    ProblemDetail errors, batch menu-item lookup.
  - order-service (8082): annotated controllers, hexagonal ports, R2DBC with a reactive transaction
    over two tables, Idempotency-Key, HTTP interface client to restaurant-service, WebClient +
    Resilience4j circuit breaker + retry/backoff to payment-service, publishes OrderPlaced to Kafka,
    consumes delivery status with an async listener, order state machine, SSE status stream (Sinks).
  - payment-service (8085): simulated latency (Mono.delay), configurable failure rate, idempotent
    replays via Mono.cache().
  - delivery-service (8083): reactive MongoDB, consumes OrderPlaced, assigns a courier, simulates the
    trip with Flux.interval + concatMap, publishes status events, SSE tracking.
  - notification-service (8084): consumes all events, fans out through a replaying Sink to WebSocket
    clients (with SSE fallback).
  - common-events: framework-free event records + topic names (food.<context>.<aggregate>.<event>.v1).
  Money is long minor units (paise), never floating point. Timestamps are Instant/TIMESTAMPTZ UTC.
  Provide docker-compose (Postgres 17, MongoDB 8, Kafka KRaft; apps behind a profile) and a
  multi-stage Dockerfile. Unit/slice tests must run without Docker.

QUALITY BAR
- Constructor injection, records, jakarta.* imports, no field injection, no block() in request paths.
- `./gradlew build` is green; `mkdocs build --strict` is green.
- Verify the capstone end to end (place order → payment → Kafka → courier simulation → SSE/WebSocket
  updates until DELIVERED) before calling it done.
- README with quick start; this prompt saved as MASTER_PROMPT.md and rendered as a docs page.
```

## Why the prompt is shaped this way

- **Generic first, capstone last.** Topic pages stay focused on one concept each; the food delivery app
  is where they are composed, so a reader meets each idea in isolation before seeing it under load.
- **Examples are tests.** Snippet markers + `check_paths: true` mean a renamed method or a broken example
  breaks the docs build, not the reader's trust.
- **Versions are verified, not remembered.** Reactive Spring changes fast (Boot 4 split auto-configuration
  into modules, Jackson 3 moved to `tools.jackson`, Gateway moved properties under
  `spring.cloud.gateway.server.webflux`), so the prompt insists on checking Maven Central.
