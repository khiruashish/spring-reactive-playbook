package com.example.fooddelivery.events;

import java.time.Instant;

/** Emitted by delivery-service on every status transition and location ping. */
public record DeliveryStatusChanged(
        String orderId,
        String deliveryId,
        String courierId,
        DeliveryStatus status,
        double latitude,
        double longitude,
        Instant at) {
}
