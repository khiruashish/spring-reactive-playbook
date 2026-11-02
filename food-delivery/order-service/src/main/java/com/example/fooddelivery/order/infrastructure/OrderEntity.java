package com.example.fooddelivery.order.infrastructure;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("orders")
record OrderEntity(@Id Long id, String customerId, Long restaurantId, String status, long totalPaise,
                   String deliveryAddress, String idempotencyKey, String paymentId,
                   Instant createdAt, Instant updatedAt) {
}
