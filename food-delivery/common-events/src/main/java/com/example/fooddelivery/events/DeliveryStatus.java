package com.example.fooddelivery.events;

/** Lifecycle of a delivery, in order. */
public enum DeliveryStatus {
    COURIER_ASSIGNED,
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED
}
