# Level 1 · Foundation (0–2 years)

**Goal:** think in streams, and write and test simple reactive endpoints without blocking.

## Master these

- [ ] Why reactive exists; event loop vs thread-per-request — [Why Reactive?](../fundamentals/why-reactive.md)
- [ ] Reactive Streams signals: `onSubscribe`, `onNext`, `onError`, `onComplete`, `request(n)` — [Spec](../fundamentals/reactive-streams.md)
- [ ] `Mono` vs `Flux`; **nothing happens until subscribe** — [Mono & Flux](../reactor/mono-and-flux.md)
- [ ] Creating: `just`, `empty`, `error`, `fromIterable`, `range`, `fromCallable`, `defer` — [Creating](../reactor/creating-sequences.md)
- [ ] `map` vs `flatMap`; `filter`; `defaultIfEmpty`/`switchIfEmpty` — [Transforming](../reactor/transforming.md), [Filtering](../reactor/filtering.md)
- [ ] `zip`, `then`, `collectList` — [Combining](../reactor/combining.md)
- [ ] Basic errors: `onErrorReturn`, `onErrorResume`, `onErrorMap` — [Error Handling](../reactor/error-handling.md)
- [ ] Annotated controllers returning `Mono`/`Flux` — [Annotated Controllers](../webflux/annotated-controllers.md)
- [ ] `StepVerifier` basics and `WebTestClient` — [StepVerifier](../testing/stepverifier.md), [WebTestClient](../testing/webtestclient.md)

## Exercises

1. Run `./gradlew :playbook-examples:test`. Break one expectation in `TransformingOperatorsTest` and read the failure.
2. Build a `/books` CRUD API with an in-memory reactive repository (copy the shape of `ProductController`). Return 404 via `switchIfEmpty(Mono.error(...))` + `@RestControllerAdvice`.
3. Write `StepVerifier` tests for every endpoint method and `WebTestClient` tests for the HTTP layer.
4. Add `.log()` to a pipeline and explain each line of output to a colleague.

## Checkpoint

You can explain, without notes: why `Mono.just(callDb())` is a bug, why the returned `Mono` must be
returned, and when to use `flatMap` instead of `map`.

## Typical mistakes at this level

- calling `block()` inside a controller · forgetting to return a `Mono` · `subscribe()` inside `map` ·
  testing with `Thread.sleep` instead of virtual time.
