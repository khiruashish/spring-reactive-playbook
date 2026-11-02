# R2DBC

**R2DBC** (Reactive Relational Database Connectivity) is a non-blocking SPI for SQL databases. Spring
Data R2DBC adds repositories, mapping and `R2dbcEntityTemplate` on top.

```kotlin
implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
runtimeOnly("org.postgresql:r2dbc-postgresql")
```

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/catalog
    username: app
    password: secret
    pool:
      initial-size: 5
      max-size: 20
      max-idle-time: 30m
```

## Entities are simple — records work

```java
@Table("products")
public record ProductRow(@Id Long id, String name, String category, long priceMinor, Instant createdAt) { }
```

R2DBC is **not an ORM**: no lazy loading, no cascades, no `@OneToMany`. Model aggregates explicitly and
load children with a second query (or a join via `DatabaseClient`).

## Repositories

```java
public interface ProductRowRepository extends ReactiveCrudRepository<ProductRow, Long> {

    Flux<ProductRow> findByCategoryOrderByName(String category);          // derived query

    @Query("SELECT * FROM products WHERE price_minor BETWEEN :min AND :max")
    Flux<ProductRow> findInPriceRange(long min, long max);

    @Modifying
    @Query("UPDATE products SET price_minor = :price WHERE id = :id")
    Mono<Integer> updatePrice(Long id, long price);                      // rows affected
}
```

!!! warning "Insert vs update"
    `save()` inserts when the `@Id` is `null` and updates otherwise. If **you** assign ids (UUIDs), `save()`
    tries an UPDATE and affects 0 rows. Either let the database generate ids, implement `Persistable`
    with an `isNew()` flag, or call `R2dbcEntityTemplate.insert(entity)`.

## `R2dbcEntityTemplate` — fluent, type-safe queries

```java
Flux<ProductRow> cheap = template.select(ProductRow.class)
        .matching(query(where("category").is("books").and("price_minor").lessThan(1_000))
                .sort(by("name")).limit(20))
        .all();
```

## `DatabaseClient` — raw SQL, full control

```java
Flux<CategoryCount> counts = databaseClient.sql("""
            SELECT category, count(*) AS n FROM products GROUP BY category
            """)
        .map((row, meta) -> new CategoryCount(row.get("category", String.class), row.get("n", Long.class)))
        .all();
```

## Schema management

R2DBC has no schema tooling of its own. Options:

- **Flyway/Liquibase over JDBC** at startup (add the JDBC driver; Boot runs migrations with a separate
  JDBC `DataSource` used only for migrations) — recommended for production.
- `spring.sql.init.mode=always` with `schema.sql`/`data.sql` (demo/test only — the capstone uses this).

## Pitfalls

- Streaming large results is real streaming — keep the subscription short or the connection stays
  checked out of the pool.
- Pool exhaustion shows up as latency, not errors: watch `r2dbc.pool.*` metrics.
- Every query must be subscribed (returned) — a dropped `Mono` never executes.
