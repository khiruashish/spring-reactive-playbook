# Testing

| Level | Tool | Needs Docker? | Page |
|---|---|---|---|
| Publisher logic | `StepVerifier`, `TestPublisher`, `PublisherProbe`, virtual time | no | [StepVerifier](stepverifier.md) |
| HTTP layer | `WebTestClient` bound to controllers/routers, `@WebFluxTest` | no | [WebTestClient](webtestclient.md) |
| Data/messaging | `@DataR2dbcTest`, `@DataMongoTest`, Testcontainers | yes | [Integration Tests](integration-testing.md) |
| Non-blocking guarantee | BlockHound | no | [BlockHound](blockhound.md) |
