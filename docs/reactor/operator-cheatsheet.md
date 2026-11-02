# Operator Cheat Sheet

"I have … and I want …" — pick the operator.

## Create

| I want to … | Use |
|---|---|
| emit values I already have | `just`, `fromIterable`, `range` |
| call something lazily, per subscriber | `fromCallable`, `defer` |
| adapt a `CompletableFuture` | `Mono.fromFuture(() -> cf)` |
| adapt a callback/listener API | `Flux.create`, `Flux.push` |
| generate from state | `Flux.generate` |
| emit from imperative code | `Sinks.one()`, `Sinks.many()` |
| tick periodically | `Flux.interval`, `Mono.delay` |

## Transform

| I want to … | Use |
|---|---|
| change each value synchronously | `map` |
| call an async API per value (fast, unordered) | `flatMap(f, concurrency)` |
| … preserving order, one at a time | `concatMap` |
| … preserving order, concurrently | `flatMapSequential` |
| … keeping only the latest | `switchMap` |
| map and filter at once | `handle` |
| get a `List` / `Map` | `collectList`, `collectMap`, `collectMultimap` |
| fold / running total | `reduce` / `scan` |
| walk a tree / pagination | `expand`, `expandDeep` |
| reuse a chain | `transform`, `transformDeferred` |

## Filter

| I want to … | Use |
|---|---|
| keep matching | `filter`, `filterWhen` |
| the first N / until a condition | `take`, `takeWhile`, `takeUntil`, `takeUntilOther` |
| skip | `skip`, `skipWhile`, `skipUntil` |
| unique | `distinct`, `distinctUntilChanged` |
| a default when empty | `defaultIfEmpty`, `switchIfEmpty(Mono.defer(...))` |

## Combine

| I want to … | Use |
|---|---|
| all results together | `Mono.zip`, `zipWith`, `zipWhen` |
| everything as it arrives | `merge`, `flatMap` |
| sources one after another | `concat`, `concatWith` |
| the fastest | `firstWithValue` |
| latest of each on any change | `combineLatest` |
| wait for completion of several | `Mono.when`, `then` |

## Errors & time

| I want to … | Use |
|---|---|
| default value / fallback publisher | `onErrorReturn` / `onErrorResume` |
| translate an exception | `onErrorMap` |
| retry transient failures | `retryWhen(Retry.backoff(n, d).jitter(0.5).filter(...))` |
| a deadline | `timeout(d)` / `timeout(d, fallback)` |
| delay / pace | `delayElements`, `delaySubscription`, `sample`, `limitRate` |
| measure latency | `elapsed`, `timed` |

## Threads, sharing, side effects

| I want to … | Use |
|---|---|
| run blocking code | `fromCallable(...).subscribeOn(boundedElastic())` |
| continue on another thread | `publishOn` |
| compute once, share result | `cache` |
| multicast a live stream | `share`, `publish().autoConnect()` |
| log / metrics / side effects | `doOnNext`, `doOnError`, `doFinally`, `log`, `tap` |
| pass request-scoped data | `contextWrite`, `deferContextual` |
