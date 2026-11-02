package com.example.fooddelivery.notification;

import java.time.Instant;

public record Notification(String orderId, String type, String message, Instant at) {
}
