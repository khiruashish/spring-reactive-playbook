package com.example.fooddelivery.order.infrastructure;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

interface OrderLineEntityRepository extends ReactiveCrudRepository<OrderLineEntity, Long> {

    Flux<OrderLineEntity> findByOrderId(Long orderId);
}
