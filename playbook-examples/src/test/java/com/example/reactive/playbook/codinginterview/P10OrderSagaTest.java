package com.example.reactive.playbook.codinginterview;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class P10OrderSagaTest {

    static P10OrderSaga saga(boolean paymentFails, boolean courierFails) {
        P10OrderSaga.Inventory inv = new P10OrderSaga.Inventory() {
            public Mono<String> reserve(String o) { return Mono.just("res-" + o); }
            public Mono<Void> release(String r) { return Mono.empty(); }
        };
        P10OrderSaga.Payments pay = new P10OrderSaga.Payments() {
            public Mono<String> charge(String o) { return paymentFails ? Mono.error(new IllegalStateException("card declined")) : Mono.just("ch-" + o); }
            public Mono<Void> refund(String c) { return Mono.empty(); }
        };
        P10OrderSaga.Courier courier = o -> courierFails ? Mono.error(new IllegalStateException("no riders")) : Mono.just("bk-" + o);
        return new P10OrderSaga(inv, pay, courier);
    }

    @Test
    void happyPath_noCompensation() {
        var s = saga(false, false);
        StepVerifier.create(s.place("o1")).expectNext(new P10OrderSaga.Confirmation("res-o1", "ch-o1", "bk-o1")).verifyComplete();
        assertThat(s.compensations).isEmpty();
    }

    @Test
    void paymentFails_releasesStock_andSurfacesTheOriginalError() {
        var s = saga(true, false);
        StepVerifier.create(s.place("o2")).expectErrorMessage("card declined").verify();
        assertThat(s.compensations).containsExactly("release res-o2");
    }

    @Test
    void courierFails_refundsThenReleases_inReverseOrder() {
        var s = saga(false, true);
        StepVerifier.create(s.place("o3")).expectErrorMessage("no riders").verify();
        assertThat(s.compensations).containsExactly("refund ch-o3", "release res-o3");
    }
}
