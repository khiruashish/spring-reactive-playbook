package com.example.fooddelivery.notification;

import com.example.fooddelivery.events.DeliveryStatusChanged;
import com.example.fooddelivery.events.OrderPlaced;
import com.example.fooddelivery.events.Topics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

/** Turns domain events into human-readable notifications. */
@Component
class NotificationListeners {

    private final NotificationHub hub;
    private final JsonMapper json;

    NotificationListeners(NotificationHub hub, JsonMapper json) {
        this.hub = hub;
        this.json = json;
    }

    @KafkaListener(topics = Topics.ORDER_PLACED, groupId = "notification-service")
    public Mono<Void> onOrderPlaced(String payload) {
        return Mono.fromCallable(() -> json.readValue(payload, OrderPlaced.class))
                .map(NotificationListeners::toNotification)
                .doOnNext(hub::push)
                .then();
    }

    @KafkaListener(topics = Topics.DELIVERY_STATUS, groupId = "notification-service")
    public Mono<Void> onDeliveryStatus(String payload) {
        return Mono.fromCallable(() -> json.readValue(payload, DeliveryStatusChanged.class))
                .map(NotificationListeners::toNotification)
                .doOnNext(hub::push)
                .then();
    }

    static Notification toNotification(OrderPlaced e) {
        return new Notification(e.orderId(), "ORDER_CONFIRMED",
                "Order #%s confirmed — ₹%d.%02d paid".formatted(e.orderId(), e.totalPaise() / 100, e.totalPaise() % 100),
                e.placedAt());
    }

    static Notification toNotification(DeliveryStatusChanged e) {
        String message = switch (e.status()) {
            case COURIER_ASSIGNED -> "%s is heading to the restaurant".formatted(e.courierId());
            case PICKED_UP -> "%s picked up your food".formatted(e.courierId());
            case IN_TRANSIT -> "On the way (%.4f, %.4f)".formatted(e.latitude(), e.longitude());
            case DELIVERED -> "Delivered. Enjoy your meal!";
        };
        return new Notification(e.orderId(), e.status().name(), message, e.at());
    }
}
