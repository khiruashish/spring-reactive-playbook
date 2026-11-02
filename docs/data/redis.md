# Reactive Redis

```kotlin
implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
```

## Templates

```java
@Service
class SessionCounter {
    private final ReactiveStringRedisTemplate redis;

    SessionCounter(ReactiveStringRedisTemplate redis) { this.redis = redis; }

    Mono<Long> hit(String page) {
        return redis.opsForValue().increment("hits:" + page);
    }

    Mono<Boolean> rememberToken(String token, Duration ttl) {
        return redis.opsForValue().set("token:" + token, "1", ttl);
    }
}
```

For objects, build a `ReactiveRedisTemplate<String, T>` with a JSON serializer (`JacksonJsonRedisSerializer`
in Jackson 3 setups).

## Read-through cache

Spring's `@Cacheable` does not cache the *value* of a `Mono` reliably across all providers. A simple,
explicit pattern:

```java
Mono<Product> cached(String id) {
    String key = "product:" + id;
    return productTemplate.opsForValue().get(key)
            .switchIfEmpty(Mono.defer(() -> repository.findById(id)
                    .flatMap(p -> productTemplate.opsForValue().set(key, p, Duration.ofMinutes(10)).thenReturn(p))));
}
```

## Pub/Sub — fan-out across instances

```java
Mono<Long> publish(PriceTick tick) {
    return redis.convertAndSend("prices", json(tick));
}

Flux<String> subscribe() {
    return redis.listenToChannel("prices").map(ReactiveSubscription.Message::getMessage);
}
```

This is the simplest way to make an in-memory `Sinks.Many` (SSE/WebSocket broadcast) work across
several instances of a service.

## Rate limiting

Spring Cloud Gateway's `RequestRateLimiter` uses reactive Redis with a token-bucket Lua script — see
[Gateway](../messaging/gateway.md).
