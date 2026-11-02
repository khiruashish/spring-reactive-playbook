# Migrating from Spring MVC

## Should you?

Revisit [Reactive vs Virtual Threads](../fundamentals/reactive-vs-virtual-threads.md) first. Migrate a
service when it is I/O-bound with high concurrency, streams data, or fans out to many dependencies —
not because reactive is fashionable.

## Type mapping

| Spring MVC | WebFlux |
|---|---|
| `spring-boot-starter-web` | `spring-boot-starter-webflux` |
| `T`, `List<T>`, `ResponseEntity<T>` | `Mono<T>`, `Flux<T>`, `Mono<ResponseEntity<T>>` |
| `RestTemplate` / `RestClient` | `WebClient` / HTTP interface clients |
| `JpaRepository` | `ReactiveCrudRepository` (R2DBC/Mongo) |
| `@Transactional` (JDBC) | `@Transactional` (R2DBC) / `TransactionalOperator` |
| `HandlerInterceptor`, servlet `Filter` | `WebFilter` |
| `SecurityFilterChain`, `HttpSecurity` | `SecurityWebFilterChain`, `ServerHttpSecurity` |
| `SecurityContextHolder` | `ReactiveSecurityContextHolder` |
| `MockMvc` | `WebTestClient` |
| `SseEmitter` | `Flux<ServerSentEvent<T>>` |
| `@Async`, `CompletableFuture` | `Mono`/`Flux` composition |
| MDC | Reactor Context + context-propagation |

## Step by step

1. **Edge first**: introduce `WebClient` for outbound calls inside the MVC app (it works there too).
2. **Data access**: replace JDBC/JPA with R2DBC in one bounded context; keep Flyway on JDBC for migrations.
3. **Switch the web stack**: swap the starter, convert controllers bottom-up (repository → service → controller).
4. **Security & filters**: port to the reactive equivalents above.
5. **Tests**: `MockMvc` → `WebTestClient`, add `StepVerifier` tests for services, run BlockHound.
6. **Load test** and compare with the baseline before decommissioning the old version.

!!! warning "Half-migrated services are the worst of both worlds"
    A WebFlux controller calling a blocking JPA repository through `boundedElastic` gives you reactive
    complexity *and* thread-pool limits. Finish the data layer or stay on MVC.
