package com.example.fooddelivery.order.application;

import com.example.fooddelivery.order.domain.OrderStatus;
import java.time.Instant;

/** What a client watching an order's live status stream receives. Coordinates are null until a courier moves. */
public record OrderStatusUpdate(long orderId, OrderStatus status, Double latitude, Double longitude, Instant at) {
}
