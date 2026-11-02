package com.example.fooddelivery.order.domain;

import java.time.Instant;
import java.util.List;

/** The order aggregate as the application sees it — persistence mapping lives in infrastructure. */
public record Order(
        Long id,
        String customerId,
        Long restaurantId,
        OrderStatus status,
        long totalPaise,
        String deliveryAddress,
        String idempotencyKey,
        String paymentId,
        Instant createdAt,
        List<OrderLine> lines) {

    public Order withStatus(OrderStatus next, String paymentId) {
        return new Order(id, customerId, restaurantId, next, totalPaise, deliveryAddress,
                idempotencyKey, paymentId, createdAt, lines);
    }

    public Order withLines(List<OrderLine> newLines) {
        return new Order(id, customerId, restaurantId, status, totalPaise, deliveryAddress,
                idempotencyKey, paymentId, createdAt, List.copyOf(newLines));
    }
}
