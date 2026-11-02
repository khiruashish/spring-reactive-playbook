package com.example.fooddelivery.order.infrastructure;

import java.time.Instant;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface OrderEntityRepository extends ReactiveCrudRepository<OrderEntity, Long> {

    Mono<OrderEntity> findByIdempotencyKey(String idempotencyKey);

    Flux<OrderEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    @Modifying
    @Query("UPDATE orders SET status = :status, payment_id = :paymentId, updated_at = :at WHERE id = :id")
    Mono<Integer> updateStatus(Long id, String status, String paymentId, Instant at);
}
