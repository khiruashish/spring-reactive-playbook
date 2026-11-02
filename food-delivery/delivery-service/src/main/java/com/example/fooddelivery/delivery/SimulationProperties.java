package com.example.fooddelivery.delivery;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Courier simulation: how often a courier "moves" and how many hops a trip takes. */
@ConfigurationProperties("simulation")
public record SimulationProperties(Duration tick, int steps, List<String> couriers) {
}
