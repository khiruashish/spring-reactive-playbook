# Mono & Flux

| Type | Emits | Typical use |
|---|---|---|
| `Mono<T>` | 0 or 1 element, then complete — or an error | a single HTTP response, `findById`, a save |
| `Flux<T>` | 0..N elements (possibly infinite), then complete — or an error | query results, event streams, SSE |
| `Mono<Void>` | no element — only completion or error | "done" signals: delete, send, write response |

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/CreatingSequencesTest.java:basics"
```

## Assembly time vs subscription time

Writing `Flux.just(1,2,3).map(...)` **assembles** a pipeline — a description. Nothing executes until
something **subscribes**. In WebFlux, the framework subscribes for you when it writes the response.

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/CreatingSequencesTest.java:lazy"
```

!!! danger "The #1 beginner bug: nobody subscribed"
    ```java
    public Mono<Void> delete(String id) {
        repository.deleteById(id);          // ❌ returned Mono is dropped → nothing is deleted
        return Mono.empty();
    }
    public Mono<Void> delete(String id) {
        return repository.deleteById(id);   // ✅ framework subscribes to what you return
    }
    ```

## Cold by default

Each subscription **re-executes** the pipeline from the source (a new HTTP call, a new query). That is
usually what you want; when it is not, see [Hot vs Cold & Sinks](hot-vs-cold-and-sinks.md).

## Converting between them

| From → To | Operator |
|---|---|
| `Flux` → `Mono<List>` | `collectList()`, `collectMap()`, `collectMultimap()` |
| `Flux` → `Mono` (one element) | `next()`, `last()`, `elementAt(i)`, `single()` |
| `Flux` → `Mono` (aggregate) | `reduce()`, `count()`, `hasElements()`, `all()`, `any()` |
| `Flux` → `Mono<Void>` | `then()` |
| `Mono` → `Flux` | `flux()`, `flatMapMany()`, `thenMany()` |

## Subscribing

```java
flux.subscribe();                                       // fire and forget (rarely correct in a server)
flux.subscribe(v -> log.info("{}", v));                 // onNext
flux.subscribe(v -> {}, err -> log.error("", err));     // + onError — ALWAYS handle errors
flux.subscribe(v -> {}, err -> {}, () -> log.info("done"));
Disposable d = flux.subscribe(); d.dispose();           // cancel
```

!!! warning "`block()` belongs in tests, `main` methods and batch jobs — never on an event loop"
    Calling `block()`/`blockFirst()`/`blockLast()` inside a WebFlux handler freezes a Netty thread and
    Reactor throws `IllegalStateException: block()/blockFirst()/blockLast() are blocking, which is not
    supported in thread reactor-http-nio-*`.
