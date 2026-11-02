# Level 3 · Senior (5–8 years)

**Goal:** own reactive services in production: event-driven integration, resilience, observability,
performance.

## Master these

- [ ] Backpressure strategies and batching; designing for slow consumers — [Backpressure & Batching](../reactor/backpressure-and-batching.md)
- [ ] Reactor `Context`, context propagation, MDC/tracing — [Context](../reactor/context.md), [Observability](../resilience/observability.md)
- [ ] Resources and cancellation semantics (`usingWhen`, `Disposable`) — [Resources](../reactor/resources-and-cancellation.md)
- [ ] Kafka with async listeners, idempotent consumers, outbox — [Kafka](../messaging/kafka.md)
- [ ] Spring Cloud Gateway, rate limiting — [Gateway](../messaging/gateway.md)
- [ ] Circuit breakers, bulkheads, hedging — [Resilience Patterns](../resilience/resilience-patterns.md)
- [ ] Performance tuning: event loops, pools, memory, codecs — [Performance](../production/performance-tuning.md)
- [ ] BlockHound in CI; debugging with checkpoints and the debug agent — [BlockHound](../testing/blockhound.md), [Debugging](../reactor/debugging.md)
- [ ] Method security, OAuth2 client token relay — [Method Security](../security/method-security.md)
- [ ] Scheduling and lifecycle of background streams — [Scheduling & Lifecycle](../production/scheduling-and-lifecycle.md)
- [ ] Streaming AI responses, GraphQL subscriptions — [AI Streaming](../messaging/ai-streaming.md), [GraphQL](../webflux/graphql.md)

## Exercises

1. Trace one order through the [Food Delivery App](../food-delivery/index.md); set `PAYMENT_FAILURE_RATE=0.8` and explain every log line until the breaker opens.
2. Add an outbox table to order-service and relay it with a poller (`@Scheduled` returning `Mono`).
3. Make SSE fan-out work across two instances (Kafka group per instance or Redis pub/sub).
4. Load test an endpoint with k6; find the bottleneck using metrics only.
5. Solve [coding exercises](../interview/coding-exercises.md) 7–10.

## Checkpoint

You can explain a production incident in reactive terms (who requested what, where the signal stopped,
which pool was exhausted) and design the fix.
