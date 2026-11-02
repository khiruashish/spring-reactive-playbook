package com.example.fooddelivery.payment;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/** A fake card processor: non-blocking latency via {@code Mono.delay}, random outages, idempotent replays. */
@Service
public class PaymentGateway {

    public enum Status { CAPTURED, DECLINED }

    public record ChargeRequest(String orderRef, String customerId, long amountPaise) {
    }

    public record Payment(String paymentId, String orderRef, long amountPaise, Status status, Instant at) {
    }

    private final PaymentProperties props;
    private final DoubleSupplier random;
    /** Idempotency-Key → cached result. {@code Mono.cache()} replays the first outcome to every retry. */
    private final Map<String, Mono<Payment>> byIdempotencyKey = new ConcurrentHashMap<>();

    @Autowired // two constructors: tell Spring which one to use (the other is a test seam)
    public PaymentGateway(PaymentProperties props) {
        this(props, Math::random);
    }

    PaymentGateway(PaymentProperties props, DoubleSupplier random) {
        this.props = props;
        this.random = random;
    }

    public Mono<Payment> charge(String idempotencyKey, ChargeRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return process(request);
        }
        return byIdempotencyKey.computeIfAbsent(idempotencyKey, key -> process(request)
                // failures must not be cached — let the client retry them
                .doOnError(e -> byIdempotencyKey.remove(key))
                .cache());
    }

    private Mono<Payment> process(ChargeRequest request) {
        return Mono.delay(props.latency())
                .flatMap(tick -> random.getAsDouble() < props.failureRate()
                        ? Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "processor unavailable"))
                        : Mono.just(new Payment(
                                UUID.randomUUID().toString(),
                                request.orderRef(),
                                request.amountPaise(),
                                request.amountPaise() > props.limitPaise() ? Status.DECLINED : Status.CAPTURED,
                                Instant.now())));
    }
}
