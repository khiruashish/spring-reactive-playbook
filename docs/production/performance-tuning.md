# Performance Tuning

## Measure first

Load test with realistic concurrency (Gatling, k6, wrk2) and watch: p99 latency, error rate, CPU,
event-loop pending tasks, pool pending acquisitions and GC. Reactive apps usually saturate **CPU or a
pool** — rarely threads.

## Event loops

- Default `reactor.netty.ioWorkerCount` = available processors (min 4). In containers, set CPU
  **requests/limits** so the JVM sees the right core count.
- Anything slow on the event loop (big JSON, regex, crypto, logging to a blocking appender) delays
  *all* connections on it. Offload CPU-heavy steps with `publishOn(Schedulers.parallel())`.
- Use async logging (Logback `AsyncAppender` / Log4j2 async loggers).

## Connection pools

| Pool | Settings |
|---|---|
| WebClient (Reactor Netty `ConnectionProvider`) | `maxConnections` (default 2 × cores, min 16), `pendingAcquireMaxCount`, `pendingAcquireTimeout`, `maxIdleTime`, `evictInBackground` |
| R2DBC (`r2dbc-pool`) | `spring.r2dbc.pool.max-size`, `max-acquire-time`, `max-idle-time`, `validation-query` |
| Redis (Lettuce) | one multiplexed connection is usually enough; pool only for blocking/transactional commands |

Make `maxIdleTime` shorter than any load balancer idle timeout to avoid "connection reset" on reuse.

## Memory

- Bound everything: `limitRate`, bounded `onBackpressureBuffer`, `flatMap` concurrency, `bufferTimeout`.
- `spring.codec.max-in-memory-size` caps aggregated bodies; stream large payloads (`Flux<DataBuffer>`)
  instead of `bodyToMono(String.class)`.
- Leaked `DataBuffer`s show up as Netty `LEAK:` logs — run tests with `-Dio.netty.leakDetection.level=paranoid`.

## Latency

- Fan out with `Mono.zip`/`flatMap` instead of sequential `flatMap` chains.
- `cache()` hot reference data; use `Mono.fromCallable(...).cache(ttl)` for config-like lookups.
- Enable HTTP/2 (`server.http2.enabled=true`) for many concurrent requests per client.
- Set timeouts at every hop, shortest at the edge-most caller **minus** downstream budgets.

## Startup & footprint

- Functional endpoints + fewer auto-configurations start faster.
- GraalVM native images (`org.graalvm.buildtools.native` plugin + `nativeCompile`) shrink memory and
  startup dramatically; test reflection-heavy libraries carefully.
- CDS/AOT cache (JDK 24+ `-XX:AOTCache`) speeds JVM startup with no code changes.
