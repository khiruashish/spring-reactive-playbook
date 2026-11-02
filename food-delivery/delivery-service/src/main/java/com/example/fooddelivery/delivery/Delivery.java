package com.example.fooddelivery.delivery;

import com.example.fooddelivery.events.DeliveryStatus;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** MongoDB document: a natural fit for a frequently-rewritten, schema-light tracking record. */
@Document("deliveries")
public record Delivery(
        @Id String id,
        @Indexed(unique = true) String orderId,
        String courierId,
        DeliveryStatus status,
        double latitude,
        double longitude,
        Instant updatedAt) {

    public Delivery moveTo(DeliveryStatus next, double lat, double lon, Instant at) {
        return new Delivery(id, orderId, courierId, next, lat, lon, at);
    }
}
