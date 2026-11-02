# Architect (12–15+ years)

??? question "1. Reactive programming vs reactive systems — how do you explain the difference to executives?"
    Reactive *programming* is a coding technique that makes individual services handle more concurrent
    work with fewer resources. Reactive *systems* are an architecture — asynchronous messaging, isolation,
    elasticity — that keeps the **whole platform** responsive when parts fail or load spikes. You can have
    either without the other; business value comes from the system properties (availability, latency,
    cost per request), so invest in messaging, isolation and observability before rewriting code styles.

??? question "2. When would you advise against adopting reactive?"
    When workloads are CPU-bound or low-concurrency CRUD; when key dependencies are blocking with no
    reactive alternative; when the team lacks experience and timelines are tight; when virtual threads
    give the needed scalability; when operational tooling (profilers, APM) isn't ready. The cost is
    cognitive load, debugging difficulty and hiring — it must buy measurable gains.

??? question "3. Design flow control end to end, from browser to database."
    Browser: SSE/WebSocket with client-side throttling. Edge: rate limiting, connection limits, HTTP/2
    flow control. Services: bounded concurrency (`flatMap(n)`), bounded queues, `limitRate`, timeouts and
    load shedding when pending work grows. Messaging: Kafka consumers pull at their pace; lag is the
    buffer, autoscaling on lag. Database: pool limits act as backpressure — monitor pending acquisitions.
    Across all: propagate deadlines, fail fast, degrade gracefully. Reactive Streams gives in-process
    backpressure; the architecture must provide it between processes.

??? question "4. How do you guarantee consistency across services without distributed transactions?"
    Local transactions + outbox/CDC for reliable event publication; idempotent consumers; sagas with
    compensations; read models (CQRS) updated from events with eventual consistency; idempotency keys at
    API boundaries; reconciliation jobs for drift; explicit consistency SLAs in product requirements.
    "Exactly once" is achieved as effectively-once via idempotency, not by the transport.

??? question "5. How would you run live tracking for 1M concurrent viewers across regions?"
    See [System Design](system-design.md#live-tracking-at-scale): regional SSE/WebSocket edge tiers
    (WebFlux, HTTP/2), positions partitioned by courier/order in Kafka per region, edge instances
    subscribe to the subset they need (consistent hashing of order ids to instances or a pub/sub fan-out
    tier), `onBackpressureLatest` per client, sampling (1 update/s), snapshot + delta, CDN-friendly
    fallback polling, global routing to nearest region, and capacity planning by connections per core.

??? question "6. How do you set platform standards without blocking team autonomy?"
    Golden paths (templates with security, observability, error contracts, ArchUnit rules, CI checks
    including BlockHound), paved-road libraries, ADR templates with a reactive-vs-imperative decision
    tree, and fitness functions in CI — teams can leave the path with a documented reason.

??? question "7. What does observability need to look like for async, reactive platforms?"
    Distributed tracing across HTTP **and** messaging (trace context in Kafka headers), consistent
    correlation ids, SLOs per user journey (not per service), metrics with bounded cardinality, exemplars
    linking metrics to traces, event-level audit trails for sagas, consumer lag dashboards, and logs that
    carry trace ids via context propagation. Sampling strategies (tail-based) for high-volume streams.

??? question "8. How do you evaluate cost efficiency of reactive vs imperative services?"
    Measure cost per request at target p99 under realistic load: CPU, memory (heap + direct), instance
    count, plus engineering cost (delivery speed, incident MTTR). Reactive often reduces memory and
    instance count for I/O-heavy services; native images reduce memory/startup further. Include
    opportunity cost of complexity.

??? question "9. Which failure modes are unique to reactive systems?"
    Event-loop starvation from a single blocking call; silent work loss from unsubscribed publishers or
    detached subscriptions; unbounded buffers in hot sources; retry storms amplified by high concurrency;
    context loss (security/tenant/tracing) across async boundaries; cancellation not releasing resources;
    hard-to-read stack traces slowing incident response.

??? question "10. How do you plan for long-lived connections (SSE/WebSocket) in capacity and deployments?"
    Size by concurrent connections and memory per connection; spread across instances with load balancers
    that support long-lived connections; connection draining on deploy (complete streams, clients
    reconnect with resume tokens); limit per-user connections; heartbeats; prefer HTTP/2; separate
    streaming tier from request/response tier for independent scaling.

??? question "11. How do you think about API design for streaming?"
    Choose transport by direction and audience (SSE for browser push, NDJSON for service streaming,
    WebSocket/RSocket for bidirectional), define resumption semantics (event ids/offsets), message
    schemas and versioning, backpressure expectations, and termination (complete vs infinite). Provide a
    non-streaming fallback for constrained clients.

??? question "12. What is your stance on reactive in the data layer (R2DBC) vs JDBC on virtual threads?"
    R2DBC fits fully reactive services with streaming reads and high concurrency; ecosystem gaps (no ORM,
    fewer tools) are real. JDBC on virtual threads keeps JPA/tooling and scales well for typical OLTP;
    pool size remains the true limit either way. Decide per service; never mix blocking JDBC on event
    loops.

??? question "13. How do you de-risk a platform-wide Spring Boot major upgrade?"
    Inventory services and dependency graph; upgrade shared libraries/BOM first; automated recipes
    (OpenRewrite); compatibility test matrix; canary services; feature flags for behaviour changes;
    observability baselines to compare; a time-boxed parallel-support window; communication plan.

??? question "14. How do you mentor senior engineers into architects in this domain?"
    Give them ownership of cross-service flows (sagas, streaming tiers), require written ADRs with
    measurements, rotate them through incident reviews, have them run design reviews using the reactive
    manifesto lens, and pair on capacity planning and cost analysis.

??? question "15. What will change the reactive landscape in the next few years, and how do you prepare?"
    Virtual threads and structured concurrency reducing the need for reactive in request/response
    services; reactive remaining strong for streaming, backpressure and event processing; AI token
    streaming increasing streaming endpoints; platform consolidation on fewer, well-paved models. Prepare
    by keeping domain logic framework-agnostic (ports & adapters), measuring rather than following
    fashion, and maintaining both golden paths.
