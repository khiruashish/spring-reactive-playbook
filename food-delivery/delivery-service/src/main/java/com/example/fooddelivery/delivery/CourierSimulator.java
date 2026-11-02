package com.example.fooddelivery.delivery;

import com.example.fooddelivery.events.DeliveryStatus;
import java.time.Duration;
import reactor.core.publisher.Flux;

/**
 * Pure, time-driven generator of a courier's trip. Because the clock comes from {@code Flux.interval},
 * tests replace minutes of wall time with {@code StepVerifier.withVirtualTime}.
 */
public final class CourierSimulator {

    public record Waypoint(DeliveryStatus status, double latitude, double longitude) {
    }

    private CourierSimulator() {
    }

    // --8<-- [start:route]
    public static Flux<Waypoint> route(double fromLat, double fromLon, double toLat, double toLon,
                                       int steps, Duration tick) {
        return Flux.interval(tick)
                .take(steps + 1L)                       // tick 0 = pickup … tick N = drop-off
                .map(Long::intValue)
                .map(i -> {
                    double f = (double) i / steps;      // linear interpolation along the route
                    DeliveryStatus status = i == 0 ? DeliveryStatus.PICKED_UP
                            : i == steps ? DeliveryStatus.DELIVERED
                            : DeliveryStatus.IN_TRANSIT;
                    return new Waypoint(status, fromLat + (toLat - fromLat) * f, fromLon + (toLon - fromLon) * f);
                });
    }
    // --8<-- [end:route]
}
