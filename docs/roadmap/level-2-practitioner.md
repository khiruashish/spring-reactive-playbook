# Level 2 · Practitioner (2–5 years)

**Goal:** build complete services: data access, downstream calls, security, streaming — all non-blocking.

## Master these

- [ ] `flatMap` vs `concatMap` vs `flatMapSequential` vs `switchMap`; bounded concurrency — [Transforming](../reactor/transforming.md)
- [ ] Schedulers: `publishOn` vs `subscribeOn`; wrapping blocking calls on `boundedElastic` — [Schedulers](../reactor/schedulers.md)
- [ ] Retries with backoff and filters; timeouts — [Error Handling](../reactor/error-handling.md), [Time Operators](../reactor/time-operators.md)
- [ ] Functional endpoints and `WebFilter` — [Functional Endpoints](../webflux/functional-endpoints.md), [Filters](../webflux/filters.md)
- [ ] ProblemDetail error design — [Error Handling](../webflux/error-handling.md)
- [ ] WebClient and HTTP interface clients — [WebClient](../webflux/webclient.md), [HTTP Interface Clients](../webflux/http-interface-clients.md)
- [ ] R2DBC repositories, `DatabaseClient`, reactive transactions — [R2DBC](../data/r2dbc.md), [Transactions](../data/transactions.md)
- [ ] Reactive MongoDB or Redis — [MongoDB](../data/mongodb.md), [Redis](../data/redis.md)
- [ ] Spring Security for WebFlux, JWT resource server — [WebFlux Security](../security/webflux-security.md), [OAuth2 & JWT](../security/oauth2-jwt.md)
- [ ] SSE and WebSocket — [Streaming](../webflux/streaming-sse.md), [WebSocket](../webflux/websocket.md)
- [ ] Hot vs cold, `cache()`, `share()`, Sinks — [Hot vs Cold](../reactor/hot-vs-cold-and-sinks.md)
- [ ] Integration tests with Testcontainers — [Integration Tests](../testing/integration-testing.md)

## Exercises

1. Back the `/books` API with R2DBC/PostgreSQL; add a Testcontainers `@ServiceConnection` IT.
2. Add `/books/{id}/reviews` that aggregates a second service via WebClient with timeout + retry; test with an `ExchangeFunction` stub.
3. Secure writes with JWT scopes; test with `mockJwt()`.
4. Stream new books over SSE using a `Sinks.Many`; consume it with `WebClient` in a test.
5. Solve [coding exercises](../interview/coding-exercises.md) 1–6.

## Checkpoint

You can review a pull request and spot: hidden blocking calls, unbounded `flatMap`, retried non-idempotent
calls, lost errors in detached `subscribe()`s, and `ThreadLocal` usage.
