package com.example.fooddelivery.order.domain;

import java.util.EnumSet;
import java.util.Set;

/** Order state machine. Transitions are validated so out-of-order Kafka events cannot regress an order. */
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PAYMENT_FAILED,
    COURIER_ASSIGNED,
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED;

    public boolean canTransitionTo(OrderStatus next) {
        return allowedNext().contains(next);
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == PAYMENT_FAILED;
    }

    private Set<OrderStatus> allowedNext() {
        return switch (this) {
            case PENDING_PAYMENT -> EnumSet.of(PAID, PAYMENT_FAILED);
            case PAID -> EnumSet.of(COURIER_ASSIGNED, PICKED_UP, IN_TRANSIT, DELIVERED);
            case COURIER_ASSIGNED -> EnumSet.of(PICKED_UP, IN_TRANSIT, DELIVERED);
            case PICKED_UP -> EnumSet.of(IN_TRANSIT, DELIVERED);
            case IN_TRANSIT -> EnumSet.of(IN_TRANSIT, DELIVERED); // IN_TRANSIT repeats carry location pings
            case DELIVERED, PAYMENT_FAILED -> EnumSet.noneOf(OrderStatus.class);
        };
    }
}
