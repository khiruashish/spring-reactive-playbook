# GraphQL (Spring for GraphQL)

Spring for GraphQL runs on WebFlux transparently: controller methods may return `Mono`/`Flux`, batch
loaders are reactive, and **subscriptions** stream over WebSocket (or SSE).

```kotlin
implementation("org.springframework.boot:spring-boot-starter-graphql")
implementation("org.springframework.boot:spring-boot-starter-webflux")
```

```graphql title="src/main/resources/graphql/schema.graphqls"
type Query        { product(id: ID!): Product, products(category: String): [Product] }
type Product      { id: ID!, name: String!, priceMinor: Int!, reviews: [Review] }
type Review       { stars: Int!, text: String }
type Subscription { priceChanged(productId: ID!): PriceTick }
type PriceTick    { productId: ID!, priceMinor: Int!, sequence: Int! }
```

```java
@Controller
class ProductGraphqlController {

    @QueryMapping
    Mono<Product> product(@Argument String id) { return products.findById(id); }

    @QueryMapping
    Flux<Product> products(@Argument String category) { return products.findByCategory(category); }

    /** Solves N+1: one call for all products in the response. */
    @BatchMapping
    Mono<Map<Product, List<Review>>> reviews(List<Product> products) {
        return reviews.findByProductIds(products.stream().map(Product::id).toList())
                .collectMultimap(Review::productId)
                .map(byId -> products.stream().collect(Collectors.toMap(p -> p,
                        p -> List.copyOf(byId.getOrDefault(p.id(), List.of())))));
    }

    @SubscriptionMapping
    Flux<PriceTick> priceChanged(@Argument String productId) { return prices.stream(productId); }
}
```

```yaml
spring:
  graphql:
    websocket:
      path: /graphql          # enables subscriptions over graphql-ws
    graphiql:
      enabled: true
```

Test with `GraphQlTester` / `WebGraphQlTester`:

```java
graphQlTester.document("{ product(id: \"1\") { name } }")
        .execute()
        .path("product.name").entity(String.class).isEqualTo("Keyboard");
```
