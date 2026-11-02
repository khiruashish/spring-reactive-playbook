package com.example.fooddelivery.order.infrastructure;

import com.example.fooddelivery.order.domain.Ports.Payments;
import java.time.Duration;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Payment adapter: WebClient + per-attempt timeout + retry on 5xx (safe because we send an Idempotency-Key)
 * wrapped in a Resilience4j circuit breaker so a dead processor fails fast instead of piling up requests.
 */
@Component
class HttpPayments implements Payments {

    record ChargeBody(String orderRef, String customerId, long amountPaise) {
    }

    record PaymentResponse(String paymentId, String status) {
    }

    private final WebClient paymentClient;
    private final ReactiveCircuitBreaker breaker;

    HttpPayments(WebClient paymentClient, ReactiveCircuitBreakerFactory<?, ?> breakers) {
        this.paymentClient = paymentClient;
        this.breaker = breakers.create("payment");
    }

    // --8<-- [start:charge]
    @Override
    public Mono<Receipt> charge(String idempotencyKey, String orderRef, String customerId, long amountPaise) {
        Mono<Receipt> call = paymentClient.post()
                .uri("/api/payments")
                .header("Idempotency-Key", idempotencyKey == null ? orderRef : idempotencyKey)
                .bodyValue(new ChargeBody(orderRef, customerId, amountPaise))
                .retrieve()
                .bodyToMono(PaymentResponse.class)
                .map(r -> new Receipt(r.paymentId(), Outcome.valueOf(r.status())))
                .timeout(Duration.ofSeconds(2))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(200))
                        .filter(HttpPayments::isRetryable));
        return breaker.run(call, error -> Mono.error(new IllegalStateException("payment unavailable", error)));
    }
    // --8<-- [end:charge]

    private static boolean isRetryable(Throwable e) {
        return e instanceof WebClientResponseException w && w.getStatusCode().is5xxServerError()
                || e instanceof java.util.concurrent.TimeoutException;
    }

}
