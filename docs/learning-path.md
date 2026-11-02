# Learning Path

A compact reading order with a checkpoint per stage. For the full beginner → 15+ years plan with
exercises per level, see the [Career Roadmap](roadmap/index.md).

## Stage 1 — Think in streams (1–2 days)

- [ ] [Why reactive?](fundamentals/why-reactive.md) — explain thread-per-request vs event loop
- [ ] [Reactive Streams](fundamentals/reactive-streams.md) — name the 4 interfaces and the signals
- [ ] [Backpressure](fundamentals/backpressure.md) — explain `request(n)` in your own words
- [ ] [Reactive vs virtual threads](fundamentals/reactive-vs-virtual-threads.md)

**Checkpoint:** you can say *why* a `Flux` does nothing until subscribed.

## Stage 2 — Reactor fluency (3–5 days)

- [ ] [Mono & Flux](reactor/mono-and-flux.md), [Creating](reactor/creating-sequences.md)
- [ ] [Transforming](reactor/transforming.md) — `map` vs `flatMap` vs `concatMap` vs `switchMap`
- [ ] [Filtering](reactor/filtering.md), [Combining](reactor/combining.md)
- [ ] [Error handling](reactor/error-handling.md) — retries with backoff, timeouts
- [ ] [Schedulers](reactor/schedulers.md) — `publishOn` vs `subscribeOn`, wrapping blocking calls
- [ ] [Hot vs cold & Sinks](reactor/hot-vs-cold-and-sinks.md), [Context](reactor/context.md)
- [ ] [Testing with StepVerifier](testing/stepverifier.md)

**Checkpoint:** run `./gradlew :playbook-examples:test`, then change an expectation and watch it fail.

## Stage 3 — Build reactive services (1 week)

- [ ] [WebFlux architecture](webflux/architecture.md) and [setup](webflux/project-setup.md)
- [ ] [Annotated](webflux/annotated-controllers.md) and [functional](webflux/functional-endpoints.md) endpoints
- [ ] [WebClient](webflux/webclient.md), [HTTP interface clients](webflux/http-interface-clients.md)
- [ ] [SSE](webflux/streaming-sse.md), [WebSocket](webflux/websocket.md)
- [ ] [R2DBC](data/r2dbc.md), [MongoDB](data/mongodb.md), [transactions](data/transactions.md)
- [ ] [Security](security/webflux-security.md), [JWT](security/oauth2-jwt.md)

## Stage 4 — Distributed and production-ready (1 week)

- [ ] [Kafka](messaging/kafka.md), [Gateway](messaging/gateway.md)
- [ ] [Resilience](resilience/resilience-patterns.md), [Observability](resilience/observability.md)
- [ ] [Integration tests](testing/integration-testing.md), [BlockHound](testing/blockhound.md)
- [ ] [Performance](production/performance-tuning.md), [Anti-patterns](production/best-practices.md)

## Stage 5 — Capstone

- [ ] Run the [Food Delivery App](food-delivery/index.md) and trace one order end to end
- [ ] Add a feature: e.g. order cancellation that cancels the courier simulation (`Disposable`)
