# Other Reactive Stores, Sessions & Caching

## More reactive Spring Data modules

| Store | Starter | Notes |
|---|---|---|
| Cassandra | `spring-boot-starter-data-cassandra-reactive` | `ReactiveCassandraRepository`, great for write-heavy time series |
| Elasticsearch | `spring-boot-starter-data-elasticsearch` | `ReactiveElasticsearchOperations` |
| Neo4j | `spring-boot-starter-data-neo4j` | `ReactiveNeo4jRepository`, reactive transactions |
| Couchbase | `spring-boot-starter-data-couchbase-reactive` | |

The programming model is the same everywhere: repositories return `Mono`/`Flux`, templates offer
fluent queries, and a `ReactiveTransactionManager` where the store supports transactions.

## Reactive caching

Spring's `@Cacheable` supports `Mono`/`Flux` return types (Framework 6.1+) **when the cache provider
is asynchronous** — e.g. Caffeine with `AsyncCache` mode:

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=10m
```

```java
@Bean
CaffeineCacheManager cacheManager() {
    CaffeineCacheManager manager = new CaffeineCacheManager();
    manager.setAsyncCacheMode(true);                 // required for reactive @Cacheable
    return manager;
}

@Cacheable("products")
public Mono<Product> findProduct(String id) { return repository.findById(id); }
```

Alternatives: `Mono.cache(ttl)` for single values, a Redis read-through (see [Redis](redis.md)), or
`CacheMono`-style manual lookups.

## Reactive sessions

```kotlin
implementation("org.springframework.session:spring-session-data-redis")
```

```java
@EnableRedisWebSession
class SessionConfig { }

@GetMapping("/cart/count")
Mono<Integer> cartCount(WebSession session) {
    return Mono.justOrEmpty(session.<Integer>getAttribute("cartCount")).defaultIfEmpty(0);
}
```

`WebSession` is the reactive counterpart of `HttpSession`; with Spring Session it is shared across
instances.
