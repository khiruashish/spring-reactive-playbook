package com.example.fooddelivery.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.fooddelivery.events.DeliveryStatus;
import com.example.fooddelivery.events.DeliveryStatusChanged;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class NotificationHubTest {

    @Test
    void lateSubscriberSeesRecentNotificationsForItsOrderOnly() {
        var hub = new NotificationHub();
        hub.push(new Notification("1", "ORDER_CONFIRMED", "one", Instant.now()));
        hub.push(new Notification("2", "ORDER_CONFIRMED", "two", Instant.now()));

        StepVerifier.create(hub.stream("2").take(2))
                .assertNext(n -> assertThat(n.message()).isEqualTo("two"))
                .then(() -> hub.push(new Notification("2", "DELIVERED", "done", Instant.now())))
                .assertNext(n -> assertThat(n.type()).isEqualTo("DELIVERED"))
                .verifyComplete();
    }

    @Test
    void deliveryEventsBecomeFriendlyMessages() {
        var n = NotificationListeners.toNotification(new DeliveryStatusChanged("7", "d", "courier-ravi",
                DeliveryStatus.PICKED_UP, 0, 0, Instant.now()));
        assertThat(n.message()).isEqualTo("courier-ravi picked up your food");
    }
}
