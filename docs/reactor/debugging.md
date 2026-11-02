# Debugging

Reactive stack traces show *where the operator executed* (deep inside Reactor), not *where you
assembled the pipeline*. Reactor offers three tools, from cheapest to most expensive.

## 1. `log()` — see every signal

```java
Flux.range(1, 2).map(i -> i * 10).log("calc").subscribe();
// [calc] onSubscribe(FluxMap.MapSubscriber)
// [calc] request(unbounded)
// [calc] onNext(10)
// [calc] onNext(20)
// [calc] onComplete()
```

Place `log()` right after the operator you are suspicious of; it shows demand (`request`), cancellation
and the thread each signal runs on.

## 2. `checkpoint()` — mark assembly points

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/TestingToolsTest.java:debug"
```

`checkpoint("description")` is cheap (no stack capture); `checkpoint()` without args captures a stack
trace at assembly time (more expensive).

## 3. Global assembly tracing

| Option | Cost | When |
|---|---|---|
| `Hooks.onOperatorDebug()` | high — captures a stack for **every** operator | local debugging only |
| `ReactorDebugAgent.init()` (`reactor-tools`) | low — bytecode instrumentation at load time | safe for production |
| Spring Boot: `spring.reactor.debug-agent.enabled=true` | same as agent | default `true` when `reactor-tools` is on the classpath |

```kotlin
runtimeOnly("io.projectreactor:reactor-tools")   // enables the debug agent in Spring Boot apps
```

## Other useful hooks

- `Hooks.onErrorDropped(e -> ...)` — errors that had nowhere to go (after termination, bare `subscribe()`).
- `Hooks.onNextDropped(v -> ...)` — values emitted after cancellation.
- `Hooks.enableAutomaticContextPropagation()` — copy `ThreadLocal`s (e.g. MDC, tracing) across thread
  hops; Spring Boot turns it on with `spring.reactor.context-propagation=auto`.

## A debugging checklist

1. **Nothing happens?** Somebody forgot to subscribe / return the publisher.
2. **Hangs?** A `Mono` never completes (missing `sink.complete()`, `groupBy` starvation, `Mono.never()`), or demand is 0.
3. **Runs twice?** Cold publisher subscribed twice — `cache()` or restructure.
4. **Wrong thread?** Add `.doOnNext(v -> log.info("{}", Thread.currentThread()))` around `publishOn`/`subscribeOn`.
5. **Event loop blocked?** Run tests with [BlockHound](../testing/blockhound.md).
