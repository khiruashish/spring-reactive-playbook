package com.example.fooddelivery.payment;

import com.example.fooddelivery.payment.PaymentGateway.ChargeRequest;
import com.example.fooddelivery.payment.PaymentGateway.Payment;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentGateway gateway;

    public PaymentController(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Payment> charge(@RequestHeader(name = "Idempotency-Key", required = false) String key,
                                @RequestBody ChargeRequest request) {
        return gateway.charge(key, request);
    }
}
