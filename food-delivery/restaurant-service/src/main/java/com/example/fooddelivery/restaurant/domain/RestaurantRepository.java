package com.example.fooddelivery.restaurant.domain;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface RestaurantRepository extends ReactiveCrudRepository<Restaurant, Long> {

    /** Derived query: Spring Data R2DBC generates the SQL and returns a non-blocking Flux. */
    Flux<Restaurant> findByCuisineIgnoreCase(String cuisine);
}
