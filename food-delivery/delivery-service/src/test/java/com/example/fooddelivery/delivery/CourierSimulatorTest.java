package com.example.fooddelivery.delivery;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.fooddelivery.delivery.CourierSimulator.Waypoint;
import com.example.fooddelivery.events.DeliveryStatus;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class CourierSimulatorTest {

    @Test
    void tripTakesStepsTicksAndEndsAtDestination() {
        // 4 steps × 1 minute would be 5 real minutes; virtual time makes it instantaneous
        StepVerifier.withVirtualTime(() -> CourierSimulator.route(0, 0, 4, 8, 4, Duration.ofMinutes(1)))
                .expectSubscription()
                .expectNoEvent(Duration.ofMinutes(1))
                .assertNext(wp -> assertThat(wp.status()).isEqualTo(DeliveryStatus.PICKED_UP))
                .thenAwait(Duration.ofMinutes(3))
                .expectNextMatches(wp -> wp.status() == DeliveryStatus.IN_TRANSIT && wp.latitude() == 1.0)
                .expectNextCount(2)
                .thenAwait(Duration.ofMinutes(1))
                .expectNext(new Waypoint(DeliveryStatus.DELIVERED, 4, 8))
                .verifyComplete();
    }
}
