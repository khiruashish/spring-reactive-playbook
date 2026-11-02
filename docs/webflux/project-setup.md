# Project Setup

## Gradle (Kotlin DSL) with a version catalog

```toml title="gradle/libs.versions.toml"
[versions]
springBoot = "4.1.2"

[plugins]
spring-boot = { id = "org.springframework.boot", version.ref = "springBoot" }
```

```kotlin title="build.gradle.kts"
plugins {
    java
    alias(libs.plugins.spring.boot)
}

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

dependencies {
    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-webclient")   // WebClient.Builder auto-config

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}
```

!!! info "Spring Boot 4 modularisation"
    Boot 4 split auto-configuration into focused modules. Notable for reactive apps:

    - `spring-boot-webclient` provides the auto-configured `WebClient.Builder` (it is not part of
      `spring-boot-starter-webflux` any more).
    - Test slices live in dedicated modules, e.g. `@WebFluxTest` is
      `org.springframework.boot.webflux.test.autoconfigure.WebFluxTest`.
    - Jackson 3 is the default: packages are `tools.jackson.*` (annotations stay `com.fasterxml.jackson.annotation`).

## Application class

```java
@SpringBootApplication
public class CatalogApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatalogApplication.class, args);
    }
}
```

## Useful configuration

```yaml title="application.yml"
server:
  port: 8080
  shutdown: graceful                 # finish in-flight requests on SIGTERM
spring:
  webflux:
    problemdetails:
      enabled: true                  # RFC 9457 bodies for framework errors
  codec:
    max-in-memory-size: 2MB          # limit for aggregated request bodies (default 256KB)
  lifecycle:
    timeout-per-shutdown-phase: 20s
management:
  endpoints.web.exposure.include: health,info,metrics,prometheus
```
