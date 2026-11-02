# Production Hardening

The capstone favours clarity. Before running something like it for real:

## Reliability

- [ ] **Transactional outbox** in order-service so "order saved" and "event published" can't diverge.
- [ ] **Durable idempotency** in payment-service (table with unique key) instead of an in-memory map.
- [ ] **Dead-letter topics** and bounded retries for Kafka listeners (`DefaultErrorHandler` +
      `DeadLetterPublishingRecoverer`).
- [ ] **Cross-instance fan-out**: SSE/WebSocket sinks are per instance — consume with a per-instance
      group id or use Redis pub/sub.
- [ ] Persist courier simulations' progress (or replace with real courier pings) so restarts resume trips.

## Security

- [ ] Replace the dev token endpoint with a real IdP (Keycloak/Cognito) — `issuer-uri` + JWKS.
- [ ] Make every service a resource server too (defence in depth) instead of trusting `X-User-Id` alone.
- [ ] Restrict `access_token` query parameters to SSE/WS routes; scrub them from access logs.
- [ ] Ownership checks: a customer may only read their own orders (`@PreAuthorize`).

## Data

- [ ] Flyway migrations instead of `spring.sql.init`.
- [ ] Separate DB users per service; least privilege.

## Operations

- [ ] Rate limiting at the gateway (`RequestRateLimiter` + Redis).
- [ ] Tracing (`spring-boot-starter-opentelemetry`), dashboards for RED metrics, consumer lag, breaker state.
- [ ] Graceful shutdown (`server.shutdown=graceful`), readiness/liveness probes.
- [ ] Helm charts / Kubernetes manifests with resource requests so Netty sizes event loops correctly.
