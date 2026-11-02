package com.example.fooddelivery.delivery;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface DeliveryRepository extends ReactiveMongoRepository<Delivery, String> {

    Mono<Delivery> findByOrderId(String orderId);
}
