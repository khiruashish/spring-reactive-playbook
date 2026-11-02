# Spring WebFlux

Spring WebFlux is Spring's **non-blocking web stack**, running on Reactor Netty by default. It offers
two programming models over the same engine:

| Model | Style | Page |
|---|---|---|
| Annotated controllers | `@RestController`, `@GetMapping` — familiar from Spring MVC | [Annotated Controllers](annotated-controllers.md) |
| Functional endpoints | `RouterFunction` + `HandlerFunction` — routes as code | [Functional Endpoints](functional-endpoints.md) |

On the client side, **WebClient** and **HTTP interface clients** make non-blocking calls. Beyond REST:
[GraphQL](graphql.md) (queries, batch loading, subscriptions), [Reactor Netty](reactor-netty.md) directly
(HTTP/TCP), and [file streaming with DataBuffers](files-and-databuffers.md).

The examples use a small, generic **product catalog** (`playbook-examples/src/main/java/.../web`),
tested in `WebFluxExamplesTest`.
