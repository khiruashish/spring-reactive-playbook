# Containers, Kubernetes & Native Images

## Container image

```dockerfile
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 app
USER app
COPY build/libs/app.jar /app.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app.jar"]
```

Or `./gradlew bootBuildImage` (Cloud Native Buildpacks, no Dockerfile).

## Kubernetes essentials for reactive services

| Setting | Why |
|---|---|
| CPU **requests** (and limits if you use them) | Netty sizes event loops from available processors |
| memory limit + `MaxRAMPercentage` | direct (off-heap) buffers count too — leave headroom (~25%) |
| `-XX:MaxDirectMemorySize` | cap Netty's pooled direct memory explicitly when tuning |
| liveness `/actuator/health/liveness`, readiness `/actuator/health/readiness` | probes are reactive and cheap |
| `terminationGracePeriodSeconds` > shutdown phase timeout | graceful drain |
| HPA on CPU or RPS | reactive services scale on CPU, not thread pools |

## Native images (GraalVM)

```kotlin
plugins { id("org.graalvm.buildtools.native") version "1.1.18" }
```

```bash
./gradlew nativeCompile
```

WebFlux, Reactor Netty, R2DBC, Spring Data and Spring Security are supported by Spring AOT. Expect
~50 ms startup and a fraction of the memory; pay with longer builds and closed-world restrictions
(reflection must be registered via hints). Validate with `./gradlew nativeTest`.

## Without going native

- CDS/AOT cache (JDK 24+: `-XX:AOTCacheOutput` training run, then `-XX:AOTCache`) for faster JVM startup.
- Spring AOT on the JVM (`processAot`) pre-computes bean definitions.
