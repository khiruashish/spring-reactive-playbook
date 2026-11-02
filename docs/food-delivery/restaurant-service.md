# Restaurant Service

**Role:** restaurants and menus. **Topics:** [Functional Endpoints](../webflux/functional-endpoints.md),
[R2DBC](../data/r2dbc.md), [Error Handling](../webflux/error-handling.md).

## Router

```java
--8<-- "food-delivery/restaurant-service/src/main/java/com/example/fooddelivery/restaurant/api/RestaurantRouter.java:router"
```

## Handlers

=== "list"

    ```java
    --8<-- "food-delivery/restaurant-service/src/main/java/com/example/fooddelivery/restaurant/api/RestaurantHandler.java:list"
    ```

=== "create (with validation)"

    ```java
    --8<-- "food-delivery/restaurant-service/src/main/java/com/example/fooddelivery/restaurant/api/RestaurantHandler.java:create"
    ```

## Entities are records

```java
@Table("menu_items")
public record MenuItem(@Id Long id, Long restaurantId, String name, String description,
                       long pricePaise, boolean available) { }

public interface MenuItemRepository extends ReactiveCrudRepository<MenuItem, Long> {
    Flux<MenuItem> findByRestaurantIdOrderByName(Long restaurantId);
}
```

## A bug found during end-to-end testing

order-service's HTTP interface client sends `ids` as `?ids=1&ids=2`. The first version of the handler
read only `queryParam("ids")` (the *first* value), so a two-item cart failed with *unknown menu item 2*.
The fix reads all values and still accepts the comma-separated form:

```java
List<Long> ids = request.queryParams().getOrDefault("ids", List.of()).stream()
        .flatMap(csv -> Arrays.stream(csv.split(",")))
        ...
```

Unit tests with hand-written URLs had passed — a reminder to run the services together.
