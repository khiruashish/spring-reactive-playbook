package com.example.fooddelivery.events;

import java.time.Instant;
import java.util.List;

/** Emitted by order-service once an order is persisted and its payment is captured. */
public record OrderPlaced(
        String orderId,
        String customerId,
        String restaurantId,
        long totalPaise,
        String deliveryAddress,
        List<Line> lines,
        Instant placedAt) {

    public record Line(String menuItemId, String name, int quantity, long unitPricePaise) {
    }
}
