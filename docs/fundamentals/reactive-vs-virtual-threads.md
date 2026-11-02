# Reactive vs Virtual Threads

Java 21 introduced **virtual threads** (Project Loom): threads so cheap that blocking is no longer
expensive. Does that make reactive programming obsolete? Not entirely — but it narrows where it is
the best choice.

## Side by side

=== "Spring MVC + virtual threads"

    ```java
    // application.yml: spring.threads.virtual.enabled: true
    @GetMapping("/users/{id}/view")
    UserView view(@PathVariable long id) {
        User user = users.findById(id).orElseThrow();       // blocks a *virtual* thread: cheap
        List<Order> orders = ordersClient.recent(user.id()); // RestClient, blocking, cheap
        return new UserView(user, orders);
    }
    ```

=== "Spring WebFlux + Reactor"

    ```java
    @GetMapping("/users/{id}/view")
    Mono<UserView> view(@PathVariable long id) {
        return users.findById(id)                                      // R2DBC, non-blocking
                .flatMap(user -> ordersClient.recent(user.id())         // WebClient
                        .collectList()
                        .map(orders -> new UserView(user, orders)));
    }
    ```

## Decision guide

| Concern | Virtual threads (MVC) | Reactive (WebFlux) |
|---|---|---|
| Learning curve | low — plain imperative Java | high — operators, schedulers, context |
| Debugging / stack traces | natural | needs `checkpoint()`, ReactorDebugAgent |
| Libraries | everything blocking works (JDBC, JPA) | needs non-blocking drivers (R2DBC, reactive Mongo/Redis) |
| Streaming (SSE, WebSocket, infinite streams) | possible, awkward | first-class |
| Backpressure | none built in | built in |
| Composition (timeouts, retries, races, fan-out) | `StructuredTaskScope` (preview), manual | declarative operators |
| Memory per in-flight request | small (virtual stack) | smallest |
| Pinning pitfalls | `synchronized` + blocking pinned carriers before JDK 24 | n/a |

!!! abstract "Recommendation"
    - **Default for request/response CRUD services:** Spring MVC + virtual threads.
    - **Choose WebFlux for:** API gateways, BFFs and aggregators with heavy fan-out, streaming
      endpoints (SSE/WebSocket/NDJSON), event-stream processing, and anywhere backpressure matters.
    - **They mix:** Reactor can run blocking work on virtual threads
      (`Schedulers.fromExecutorService(Executors.newVirtualThreadPerTaskExecutor())`), and with
      `spring.threads.virtual.enabled=true` Boot backs `boundedElastic` with virtual threads.

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/reactor/SchedulersTest.java:virtual"
```
