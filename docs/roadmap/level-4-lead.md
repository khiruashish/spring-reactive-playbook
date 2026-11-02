# Level 4 · Lead (8–12 years)

**Goal:** design multi-service reactive solutions, set team standards, and lead migrations.

## Master these

- [ ] Reactor internals: assembly/subscription/execution, fusion, prefetch, custom operators — [Advanced Internals](../reactor/advanced-internals.md)
- [ ] Choosing models per service: WebFlux vs MVC + virtual threads vs coroutines — [Reactive vs Virtual Threads](../fundamentals/reactive-vs-virtual-threads.md), [Kotlin](../production/kotlin-coroutines.md)
- [ ] Migration strategy from MVC — [Migrating from MVC](../production/migrating-from-mvc.md)
- [ ] Event-driven design: contracts, versioning, ordering, idempotency, sagas — [Kafka](../messaging/kafka.md), [Cloud Stream](../messaging/cloud-stream.md)
- [ ] Ports & adapters to keep domain logic free of Reactor — [Order Service](../food-delivery/order-service.md)
- [ ] Testing strategy across levels (unit, slice, IT, contract) — [Testing](../testing/index.md)
- [ ] Deployment: Kubernetes sizing for Netty, native images — [Deployment](../production/deployment.md)
- [ ] Interop: RxJava/Mutiny/Flow, `ReactiveAdapterRegistry`
- [ ] Code-review checklists and coding standards — [Best Practices](../production/best-practices.md)

## Exercises

1. Write your team's reactive coding standard (one page) based on [Best Practices](../production/best-practices.md).
2. Produce an ADR: "WebFlux or MVC + virtual threads for service X", with measurements.
3. Design order cancellation for the capstone: cancel the courier simulation (`Disposable`), refund the payment (idempotent), publish `OrderCancelled`, and handle races with delivery events.
4. Run a brown-bag session teaching `flatMap` vs `concatMap` using the tested examples.

## Checkpoint

You can design a reactive service end to end, defend its trade-offs to non-reactive peers, and grow
Level 1–3 engineers.
