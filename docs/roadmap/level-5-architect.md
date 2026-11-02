# Level 5 · Architect (12–15+ years)

**Goal:** shape platform-wide architecture: reactive systems (not just reactive code), organisational
adoption, and long-term cost of ownership.

## Master these

- [ ] Reactive Manifesto as an architecture review lens — [Reactive Manifesto](../fundamentals/reactive-manifesto.md)
- [ ] Where reactive belongs in a platform: edge, BFF, streaming, event processing — and where it doesn't
- [ ] End-to-end flow control: TCP, HTTP/2, RSocket, Kafka consumer pacing, client backpressure — [Backpressure](../fundamentals/backpressure.md), [RSocket](../messaging/rsocket.md)
- [ ] Consistency across services: outbox, CDC, sagas, idempotency keys, exactly-once myths
- [ ] Multi-region / failure-domain design: timeouts budgets, hedging, load shedding, graceful degradation
- [ ] Observability as architecture: SLOs, tracing across async boundaries, cardinality budgets
- [ ] Cost: CPU vs memory efficiency, native images, scale-to-zero, team cognitive load
- [ ] Technology radar: virtual threads, structured concurrency, Reactor and Spring release roadmaps, Kotlin coroutines

## Exercises

1. Review the [Food Delivery App](../food-delivery/index.md) as an architect: list the top 5 risks and the
   [hardening plan](../food-delivery/production-hardening.md) in priority order with effort estimates.
2. Design live courier tracking for 1M concurrent viewers across 3 regions (see
   [System Design questions](../interview/system-design.md)).
3. Write the organisation's "reactive adoption guide": decision tree, golden paths, templates, training plan.

## Checkpoint

You can state, with evidence, when reactive is the right tool for a platform — and when it is an
expensive mistake — and you can make teams successful with either choice.
