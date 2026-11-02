package com.example.fooddelivery.payment;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Knobs for the simulated payment gateway, so the order-service circuit breaker can be exercised.
 *
 * @param latency       simulated provider latency
 * @param failureRate   fraction (0..1) of calls that fail with 503
 * @param limitPaise    amounts above this are declined (business failure, not a technical one)
 */
@ConfigurationProperties("payment")
public record PaymentProperties(Duration latency, double failureRate, long limitPaise) {
}
