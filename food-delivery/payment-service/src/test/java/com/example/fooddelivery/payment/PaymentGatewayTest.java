package com.example.fooddelivery.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.fooddelivery.payment.PaymentGateway.ChargeRequest;
import com.example.fooddelivery.payment.PaymentGateway.Payment;
import com.example.fooddelivery.payment.PaymentGateway.Status;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

class PaymentGatewayTest {

    private static PaymentProperties props(double failureRate) {
        return new PaymentProperties(Duration.ofSeconds(2), failureRate, 500_000);
    }

    @Test
    void capturesWithinLimitUsingVirtualTime() {
        // withVirtualTime: the 2s Mono.delay completes instantly — no Thread.sleep in tests
        StepVerifier.withVirtualTime(() -> new PaymentGateway(props(0), () -> 1.0)
                        .charge(null, new ChargeRequest("o-1", "c-1", 45_000)))
                .expectSubscription()
                .expectNoEvent(Duration.ofSeconds(2))
                .assertNext(p -> assertThat(p.status()).isEqualTo(Status.CAPTURED))
                .verifyComplete();
    }

    @Test
    void declinesAboveLimit() {
        StepVerifier.withVirtualTime(() -> new PaymentGateway(props(0), () -> 1.0)
                        .charge(null, new ChargeRequest("o-1", "c-1", 900_000)))
                .thenAwait(Duration.ofSeconds(2))
                .assertNext(p -> assertThat(p.status()).isEqualTo(Status.DECLINED))
                .verifyComplete();
    }

    @Test
    void outageSurfacesAs503() {
        StepVerifier.withVirtualTime(() -> new PaymentGateway(props(1), () -> 0.0)
                        .charge(null, new ChargeRequest("o-1", "c-1", 100)))
                .thenAwait(Duration.ofSeconds(2))
                .expectErrorSatisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(503))
                .verify();
    }

    @Test
    void sameIdempotencyKeyReplaysTheFirstPayment() {
        var calls = new AtomicInteger();
        var gateway = new PaymentGateway(new PaymentProperties(Duration.ZERO, 0, 500_000),
                () -> { calls.incrementAndGet(); return 1.0; });
        var request = new ChargeRequest("o-1", "c-1", 100);

        Payment first = gateway.charge("key-1", request).block();
        Payment replay = gateway.charge("key-1", request).block();

        assertThat(replay).isEqualTo(first);
        assertThat(calls).hasValue(1);
    }
}
