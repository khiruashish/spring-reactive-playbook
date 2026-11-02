# Spring Reactive Playbook

A complete guide to **reactive programming with Spring**: the Reactive Streams fundamentals, Project
Reactor, Spring WebFlux, reactive data, security, messaging, resilience, testing and production. It
includes a **0 → 15+ years career roadmap** and **110 interview questions with detailed answers**, and it
ends with a **microservice food delivery app** that puts all of it together.

- **Java 21+** · **Spring Boot 4.1.2** (Spring Framework 7, Reactor 3.8, Reactor Netty 1.3) ·
  Spring Cloud 2025.1.4 · Gradle 9 (Kotlin DSL + version catalog)
- Docs are built with **MkDocs + Material for MkDocs**, with one tab per topic area and a final tab for the
  food delivery app.
- **Every Reactor/WebFlux example is a passing test.** The tests are embedded into the docs at build time,
  so the docs can't drift from working code.

## Quick start

```bash
./gradlew build                       # all examples + all services, no Docker needed
```

```bash
pip install -r requirements.txt && mkdocs serve   # http://127.0.0.1:8000
```

```bash
cd food-delivery && docker compose --profile apps up -d --build   # http://localhost:8080
```

## Layout

| Path | What |
|---|---|
| `docs/` | the playbook (MkDocs Material) |
| `playbook-examples/` | 97 tests: every Reactor/WebFlux example, interview coding exercise and "what does this print?" puzzle |
| `food-delivery/` | capstone: gateway, restaurant, order, payment, delivery and notification services, plus `common-events` |
| `MASTER_PROMPT.md` | the prompt that generates/extends this project |

## Food delivery app

```text
browser ──JWT──▶ api-gateway :8080 ──▶ restaurant-service :8081 (R2DBC/Postgres, functional endpoints)
                      │            ──▶ order-service :8082 (R2DBC, WebClient + circuit breaker, Kafka, SSE)
                      │                      └──▶ payment-service :8085 (latency/failure simulation)
                      │            ──▶ delivery-service :8083 (reactive Mongo, courier simulation, SSE)
                      └── ws ─────▶ notification-service :8084 (Kafka → Sinks → WebSocket/SSE)
```

See `docs/food-delivery/` (or the "Food Delivery App" tab) for how each service works and how to run it.
