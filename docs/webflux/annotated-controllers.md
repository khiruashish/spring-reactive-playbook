# Annotated Controllers

Annotated controllers look almost exactly like Spring MVC — the difference is the **return types**
(and that nothing may block).

```java
--8<-- "playbook-examples/src/main/java/com/example/reactive/playbook/web/ProductController.java:controller"
```

## Return types

| Return | Meaning |
|---|---|
| `Mono<T>` | single body, written when the Mono emits |
| `Flux<T>` | streamed — JSON array, or NDJSON/SSE depending on `produces`/`Accept` |
| `Mono<ResponseEntity<T>>` | control status and headers reactively |
| `ResponseEntity<Mono<T>>` / `ResponseEntity<Flux<T>>` | status/headers known up front, body streamed |
| `Mono<Void>` | empty body after completion |
| plain `T` | allowed, but computed on the event loop — fine only if trivial |

## Arguments

All the MVC arguments work (`@PathVariable`, `@RequestParam`, `@RequestHeader`, `@CookieValue`,
`@RequestBody`, `@ModelAttribute`, `Principal`), plus reactive variants:

```java
@PostMapping("/bulk")
Flux<Product> bulk(@RequestBody Flux<Product> products) {        // stream the request body
    return products.flatMap(repository::save, 8);
}

@GetMapping("/me")
Mono<String> me(Mono<Principal> principal) {                     // resolved lazily
    return principal.map(Principal::getName);
}

@GetMapping("/raw")
Mono<String> raw(ServerWebExchange exchange) {                   // full request/response access
    return Mono.just(exchange.getRequest().getPath().value());
}
```

## Validation

`@Valid @RequestBody Mono<Product>` validates during decoding and signals `WebExchangeBindException`
(→ 400). Handle it once, in an advice — see [Error Handling](error-handling.md).

## Tested

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/web/WebFluxExamplesTest.java:bindToController"
```

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/web/WebFluxExamplesTest.java:webTestClient"
```

## OpenAPI documentation

```kotlin
implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:3.2.0")   // springdoc 3.x = Boot 4
```

Swagger UI is served at `/swagger-ui.html` and the spec at `/v3/api-docs`. `Mono<T>`/`Flux<T>` are
documented as `T` / array of `T`; annotate streaming endpoints (`text/event-stream`) explicitly with
`@Operation` and `@ApiResponse(content = @Content(mediaType = "text/event-stream"))`.

## Server-side views (Thymeleaf)

WebFlux supports view rendering. With `spring-boot-starter-thymeleaf`, a controller can return a view name,
and a **data-driven** context variable streams a large `Flux` into the page in chunks:

```java
@GetMapping("/catalog")
String catalog(Model model) {
    model.addAttribute("products", new ReactiveDataDriverContextVariable(repository.findAll(), 50));
    return "catalog";   // templates/catalog.html renders rows as each batch of 50 arrives
}
```
