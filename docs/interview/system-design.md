# System Design Scenarios

Open-ended questions for senior, lead and architect interviews. Each has a model answer outline —
strong candidates go deeper on trade-offs and failure modes.

## Live tracking at scale

> Design live courier tracking for a food delivery platform: 200k active deliveries, 1M concurrent
> viewers, 3 regions. Positions arrive every 2 seconds.

??? success "Model answer"
    **Ingest:** couriers' apps send positions to a regional ingest service (WebFlux, HTTP/2 or RSocket),
    validated and published to Kafka `positions` keyed by `orderId` (≈100k msg/s). Retain a short period.

    **State:** a stream processor keeps the latest position per order (compacted topic or Redis), so new
    viewers get a snapshot instantly.

    **Fan-out tier:** stateless WebFlux "edge" instances hold SSE (or WebSocket) connections. Each
    instance needs only the orders its viewers watch — options: (a) consistent hashing routes viewers of
    an order to the same instance, which consumes the relevant partitions; (b) Redis pub/sub channels per
    order; (c) every instance consumes everything (simple, but 100k msg/s × N instances).

    **Per-connection flow control:** `snapshot.concatWith(updates).onBackpressureLatest()`, `sample(1s)`
    for slow clients, heartbeats, `takeUntil(DELIVERED)`. Capacity: ~10–50k idle SSE connections per
    instance depending on memory; scale on connections.

    **Resilience:** clients reconnect with `Last-Event-ID` → resume from latest snapshot; graceful drain on
    deploy; per-region isolation with global DNS routing; degrade to polling if the streaming tier is down.

    **Observability:** connections per instance, fan-out latency (ingest → client), Kafka lag, dropped
    updates.

## Aggregating BFF

> A mobile home screen needs data from 8 services. p99 must be under 300 ms even when one service is slow.

??? success "Model answer"
    WebFlux BFF; `Mono.zip` of mandatory parts, optional parts with `timeout(budget)` +
    `onErrorResume(empty/default)` and a response flag (`partial: true`); per-downstream connection pools
    and circuit breakers; hedged requests for idempotent reads with high tail latency; response caching
    (`Mono.cache(ttl)` or Redis) for slowly-changing parts; deadline propagation; RED metrics per
    downstream. Discuss why reactive is a good fit here (fan-out) and how virtual threads +
    `StructuredTaskScope` would compare.

## Payment flow with exactly-once effects

> Orders must be charged exactly once even with retries, timeouts and redelivered messages.

??? success "Model answer"
    Client generates an `Idempotency-Key`; order-service stores it with a unique constraint and returns
    the original result on replay; payment calls carry the key and the provider dedupes; order + outbox
    event committed in one transaction; consumers idempotent (state machine, unique keys); timeouts lead
    to *unknown* state → query the provider before retrying; reconciliation job compares ledger with
    provider reports. "Exactly once" = at-least-once delivery + idempotent effects.

## Streaming export of 50M rows

> Users can export their full history (up to 50M rows) as CSV.

??? success "Model answer"
    Never `collectList`. Either stream directly: R2DBC `Flux<Row>` → map to CSV lines → `Flux<DataBuffer>`
    response with backpressure to the DB cursor and `limitRate` for fetch batching; or (better for very
    large exports) an async job: request → job id → background pipeline writes to object storage in
    chunks (multipart upload) → notify via SSE/email with a signed URL. Consider read replicas, rate limits
    per user, cancellation (client disconnect cancels the stream), and resumability.

## Migrating a monolith's notifications to real-time

> A monolith sends emails in a nightly batch; product wants real-time push in app and web.

??? success "Model answer"
    Outbox in the monolith → CDC (Debezium) → Kafka → notification service (reactive) that renders and
    routes (push, SSE/WebSocket, email) with per-channel rate limits and preferences; replay sink for
    late joiners; delivery receipts as events; idempotent sends; gradual rollout per notification type;
    keep the batch as fallback until parity.

## Rate-limited third-party API

> A partner allows 50 requests/second; your service receives bursts of 2,000 requests/second.

??? success "Model answer"
    Decouple with a queue (Kafka) and a consumer paced to 50 rps (`zipWith(interval)`/token bucket),
    batching where the API allows; respond to callers asynchronously (202 + status endpoint/SSE); cache
    responses; prioritise traffic classes; circuit breaker on 429/5xx honouring `Retry-After`; capacity
    alerts on queue lag.
