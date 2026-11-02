# Getting Started

## Prerequisites

| Tool | Why |
|---|---|
| JDK 21+ | compiles and runs everything (25 LTS works too) |
| Docker | only for the food delivery stack and Testcontainers ITs |
| Python 3.10+ | only to serve these docs locally |

## Repository layout

```text
spring-reactive-webapp/
├── MASTER_PROMPT.md            # the prompt that generates this project
├── mkdocs.yml                  # Material for MkDocs configuration
├── docs/                       # the playbook (this site)
├── gradle/libs.versions.toml   # single source of truth for versions
├── playbook-examples/          # every Reactor/WebFlux example as a passing test
└── food-delivery/              # capstone: 6 reactive microservices + shared events
    ├── common-events/
    ├── api-gateway/            # :8080  Spring Cloud Gateway + JWT + static UI
    ├── restaurant-service/     # :8081  functional endpoints + R2DBC
    ├── order-service/          # :8082  annotated controllers + R2DBC + Kafka + circuit breaker
    ├── delivery-service/       # :8083  reactive MongoDB + Kafka + SSE
    ├── notification-service/   # :8084  Kafka → WebSocket/SSE fan-out
    ├── payment-service/        # :8085  simulated processor (latency, failures, idempotency)
    ├── docker-compose.yml
    └── Dockerfile
```

## Build and test everything

```bash
./gradlew build
```

This compiles all modules and runs every unit/slice test — none of them need Docker.

Run only the playbook examples:

```bash
./gradlew :playbook-examples:test
```

## Serve the docs

```bash
python3 -m venv .venv && . .venv/bin/activate
```

```bash
pip install -r requirements.txt
```

```bash
mkdocs serve
```

Open <http://127.0.0.1:8000>. Use `mkdocs build --strict` to produce the static site in `site/`.

## Run the capstone

See [Food Delivery App → Run It](food-delivery/running.md). In short:

```bash
cd food-delivery && docker compose --profile apps up -d --build
```

then open <http://localhost:8080>.

## How examples are embedded

Each example in the docs is a region of a test file, marked like this:

```java
// --8<-- [start:basics]
Mono<String> one = Mono.just("hello");
// --8<-- [end:basics]
```

and pulled into a page with the `pymdownx.snippets` extension:

```markdown
;--8<-- "playbook-examples/src/test/java/.../CreatingSequencesTest.java:basics"
```

`check_paths: true` in `mkdocs.yml` makes the docs build fail if a file or section goes missing.
