package com.example.fooddelivery.restaurant.domain;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface MenuItemRepository extends ReactiveCrudRepository<MenuItem, Long> {

    Flux<MenuItem> findByRestaurantIdOrderByName(Long restaurantId);
}
