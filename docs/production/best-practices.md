# Best Practices & Anti-patterns

## ❌ → ✅

| Anti-pattern | Why it hurts | Do this instead |
|---|---|---|
| `block()` in a handler | freezes an event-loop thread (and throws on Netty threads) | return the publisher; compose with operators |
| `subscribe()` inside `map`/`flatMap`/a handler | detached work: no backpressure, no cancellation, errors lost, transactions/context lost | `flatMap` it into the chain; if it must be fire-and-forget, `then()`-chain it or hand it to a managed component |
| Blocking I/O in operators (JDBC, `RestTemplate`, file I/O) | stalls the loop | non-blocking driver, or `fromCallable(...).subscribeOn(boundedElastic())` |
| `Mono.just(sideEffect())` | runs eagerly, even if never subscribed | `Mono.fromCallable` / `Mono.defer` |
| Ignoring a returned `Mono` | the operation never happens | return / chain it |
| `ThreadLocal` / MDC / `SecurityContextHolder` | values vanish across thread hops | Reactor `Context`, `ReactiveSecurityContextHolder`, context-propagation |
| Unbounded `flatMap` over large inputs | hundreds of concurrent calls, pool exhaustion | `flatMap(f, concurrency)` |
| `collectList()` on unbounded streams | OOM | stream the `Flux`; `buffer(n)` for batches |
| `onErrorContinue` everywhere | surprising semantics | per-element `onErrorResume` inside `flatMap` |
| `retry()` without backoff/filter | retry storms, duplicate side effects | `retryWhen(Retry.backoff(...).jitter(...).filter(...))` + idempotency |
| `switchIfEmpty(expensiveCall())` | `expensiveCall()` executes eagerly | `switchIfEmpty(Mono.defer(this::expensiveCall))` |
| Huge operator chains in one method | unreadable, untestable | extract named methods returning `Mono`/`Flux`, use `transform` |
| `Flux<Void>`/`Mono<Optional<T>>` | awkward semantics | `Mono<Void>`, empty `Mono<T>` |
| Mixing `@Transactional` with remote calls | holds DB connections while waiting | do remote calls outside the transaction; outbox for events |

## Habits of healthy reactive code

1. **Every public method returns `Mono`/`Flux`** (or a plain value if it does no I/O).
2. **Name things**: small methods (`loadUser`, `priceCart`) composed with `flatMap` read like a story.
3. **Decide what "empty" means** at each step (`switchIfEmpty(Mono.error(...))`, `defaultIfEmpty`).
4. **Put timeouts on every remote call** and know your error budget.
5. **Test with `StepVerifier` and virtual time**; run BlockHound in CI.
6. **Keep the domain pure** — business rules as plain Java functions, reactive code as the glue
   (see the capstone's `Pricing` and `OrderStatus` classes).
