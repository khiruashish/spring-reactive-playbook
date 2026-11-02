# Resilience & Observability

Distributed reactive systems fail partially: a dependency gets slow, a broker rebalances, a pod is
evicted. These pages cover how to **contain** failures and how to **see** what is happening.

| Page | Topics |
|---|---|
| [Resilience Patterns](resilience-patterns.md) | timeouts, retries with jitter, circuit breaker, bulkhead, rate limiter, fallbacks, hedging |
| [Observability](observability.md) | Micrometer metrics & observations, tracing, context propagation, logging/MDC, Actuator |
