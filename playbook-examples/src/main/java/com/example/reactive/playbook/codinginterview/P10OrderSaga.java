package com.example.reactive.playbook.codinginterview;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import reactor.core.publisher.Mono;

/** Problem 10: reserve stock -> charge card -> book courier; on failure, compensate completed steps in REVERSE order. */
public class P10OrderSaga {

    public interface Inventory { Mono<String> reserve(String order); Mono<Void> release(String reservation); }
    public interface Payments { Mono<String> charge(String order); Mono<Void> refund(String charge); }
    public interface Courier { Mono<String> book(String order); }

    public record Confirmation(String reservation, String charge, String booking) { }

    public final List<String> compensations = new CopyOnWriteArrayList<>();
    private final Inventory inventory; private final Payments payments; private final Courier courier;

    public P10OrderSaga(Inventory inventory, Payments payments, Courier courier) { this.inventory = inventory; this.payments = payments; this.courier = courier; }

    public Mono<Confirmation> place(String order) {
        return inventory.reserve(order).flatMap(reservation ->
                payments.charge(order)
                        .onErrorResume(e -> undo(inventory.release(reservation), "release " + reservation).then(Mono.error(e)))
                        .flatMap(charge -> courier.book(order)
                                .onErrorResume(e -> undo(payments.refund(charge), "refund " + charge)
                                        .then(undo(inventory.release(reservation), "release " + reservation))
                                        .then(Mono.error(e)))
                                .map(booking -> new Confirmation(reservation, charge, booking))));
    }

    /** Compensations must themselves be retried/idempotent in production; here they are recorded for the test. */
    private Mono<Void> undo(Mono<Void> action, String label) {
        return action.doOnSuccess(v -> compensations.add(label));
    }
}
