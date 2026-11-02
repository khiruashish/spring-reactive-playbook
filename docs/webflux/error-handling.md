# Error Handling

## Problem Details (RFC 9457)

Spring Framework returns `application/problem+json` bodies when you return a `ProblemDetail` or throw
an `ErrorResponse` exception (e.g. `ResponseStatusException`). Enable it for framework exceptions with
`spring.webflux.problemdetails.enabled=true`.

```json
{
  "type": "about:blank",
  "title": "Product not found",
  "status": 404,
  "detail": "product 42 not found",
  "instance": "/products/42"
}
```

## `@RestControllerAdvice` for annotated controllers

```java
--8<-- "playbook-examples/src/main/java/com/example/reactive/playbook/web/GlobalErrorHandler.java:advice"
```

Throw domain exceptions **inside the pipeline** (`Mono.error(...)`, `switchIfEmpty(Mono.error(...))`)
and map them centrally.

## Functional endpoints

Advice does not apply to `RouterFunction`s. Use `onError` on the router builder, or recover in the
handler:

```java
RouterFunctions.route()
        .GET("/v2/products/{id}", handler::get)
        .onError(ProductNotFoundException.class, (e, req) ->
                ServerResponse.status(HttpStatus.NOT_FOUND)
                        .bodyValue(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage())))
        .build();
```

## Global: `WebExceptionHandler`

For errors raised *outside* handlers (filters, routing), implement `WebExceptionHandler` or extend
Boot's `AbstractErrorWebExceptionHandler`, ordered before Boot's default (`@Order(-2)`).

## Checklist

- Map **expected** errors (not found, validation, conflict) to 4xx with a precise `detail`.
- Never leak stack traces or internal messages in 5xx bodies.
- Add a `type` URI per problem kind if clients branch on it.
- Log 5xx once, at the edge, with the request/trace id.
