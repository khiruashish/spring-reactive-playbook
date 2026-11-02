# Functional Endpoints

Routes are **plain values** (`RouterFunction<ServerResponse>`) built with a DSL, and handlers are
functions `ServerRequest -> Mono<ServerResponse>`. No annotations, no reflection — easy to compose,
test and even build dynamically.

## Handler

```java
--8<-- "playbook-examples/src/main/java/com/example/reactive/playbook/web/ProductRouter.java:handler"
```

## Router + filter

```java
--8<-- "playbook-examples/src/main/java/com/example/reactive/playbook/web/ProductRouter.java:router"
```

## Building blocks

| API | Purpose |
|---|---|
| `RouterFunctions.route()` | builder: `GET`, `POST`, `path`, `nest`, `filter`, `before`, `after`, `onError` |
| `RequestPredicates` | `accept(...)`, `contentType(...)`, `queryParam(...)`, `headers(...)` — compose with `and`/`or` |
| `ServerRequest` | `pathVariable`, `queryParam`, `bodyToMono`, `bodyToFlux`, `principal()`, `exchange()` |
| `ServerResponse` | `ok()`, `created(uri)`, `status(...)`, `.bodyValue(obj)`, `.body(publisher, Type.class)` |
| `HandlerFilterFunction` | per-route interception (auth, logging, timing) |

## Annotated vs functional — which one?

| Prefer annotated when … | Prefer functional when … |
|---|---|
| the team knows Spring MVC | you want explicit, testable routing |
| you want OpenAPI generation with zero config | routes are built conditionally / dynamically |
| you use rich argument resolution (`@ModelAttribute`, validation) | you want minimal reflection (fast startup, GraalVM native) |

Both can live in the same application.

## Testing without a server

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/web/WebFluxExamplesTest.java:routerTest"
```
