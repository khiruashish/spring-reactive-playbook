# BlockHound

[BlockHound](https://github.com/reactor/BlockHound) is a Java agent that **fails fast** when blocking
calls (`Thread.sleep`, socket/file I/O, `Object.wait`, `LockSupport.park`) run on threads marked
non-blocking — Reactor's `parallel`/`single` schedulers and Netty event loops.

```kotlin
testImplementation("io.projectreactor.tools:blockhound:1.0.18.RELEASE")
```

```java
class NoBlockingTest {

    @BeforeAll
    static void install() {
        BlockHound.install();                // once per JVM
    }

    @Test
    void detectsBlockingOnParallelScheduler() {
        Mono<String> bad = Mono.delay(Duration.ofMillis(1))           // runs on parallel-*
                .map(i -> { sleepQuietly(10); return "x"; });         // ❌ blocking
        StepVerifier.create(bad)
                .expectErrorMatches(e -> e.getMessage().contains("Blocking call!"))
                .verify();
    }
}
```

On JDK 13+ add `-XX:+AllowRedefinitionToAddDeleteMethods` to the test JVM:

```kotlin
tasks.test { jvmArgs("-XX:+AllowRedefinitionToAddDeleteMethods") }
```

## Allow-listing

Some libraries block briefly and harmlessly during initialisation (class loading, `SecureRandom`
seeding). Allow specific methods instead of disabling the agent:

```java
BlockHound.install(builder -> builder
        .allowBlockingCallsInside("java.util.UUID", "randomUUID"));
```

!!! tip
    Run BlockHound in CI for your WebFlux integration tests. It turns "the app is mysteriously slow
    under load" into "this test fails on line 42".
