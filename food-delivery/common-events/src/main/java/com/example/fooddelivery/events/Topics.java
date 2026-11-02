package com.example.fooddelivery.events;

/** Kafka topic names: {@code food.<context>.<aggregate>.<event>.v<version>}. */
public final class Topics {
    public static final String ORDER_PLACED = "food.order.order.placed.v1";
    public static final String DELIVERY_STATUS = "food.delivery.delivery.status-changed.v1";

    private Topics() {
    }
}
