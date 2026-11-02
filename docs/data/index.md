# Reactive Data

A reactive web layer only pays off if the data layer is non-blocking too. One blocking JDBC call on an
event loop undoes everything.

| Store | Reactive driver | Spring module | Page |
|---|---|---|---|
| PostgreSQL, MySQL, MariaDB, SQL Server, Oracle, H2 | R2DBC | Spring Data R2DBC | [R2DBC](r2dbc.md) |
| MongoDB | MongoDB Reactive Streams driver | Spring Data MongoDB (reactive) | [MongoDB](mongodb.md) |
| Redis | Lettuce | Spring Data Redis (reactive) | [Redis](redis.md) |
| Cassandra | DataStax driver | Spring Data Cassandra (reactive) | [Other stores](other-stores-and-caching.md) |
| Elasticsearch / OpenSearch | reactive clients | Spring Data Elasticsearch | [Other stores](other-stores-and-caching.md) |

!!! warning "JPA/Hibernate is blocking"
    There is no reactive JPA in Spring Data. If you must use JPA or JDBC from WebFlux, wrap calls in
    `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())` and size that pool deliberately —
    or reconsider Spring MVC + virtual threads.
