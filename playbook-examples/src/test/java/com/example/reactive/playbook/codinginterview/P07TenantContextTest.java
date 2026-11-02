package com.example.reactive.playbook.codinginterview;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

class P07TenantContextTest {

    @Test
    void contextReachesDeepOperators_evenAcrossThreadHops() {
        Mono<String> pipeline = Mono.just("c-42")
                .publishOn(Schedulers.parallel())                     // thread changes: a ThreadLocal would be lost here
                .flatMap(P07TenantContext::loadInvoices);
        StepVerifier.create(P07TenantContext.withTenant(pipeline, "acme"))
                .expectNext("SELECT * FROM acme.invoices WHERE customer='c-42'").verifyComplete();
    }

    @Test
    void downstreamCall_receivesTenantHeader() {
        AtomicReference<String> header = new AtomicReference<>();
        Mono<String> call = P07TenantContext.callBilling(tenant -> { header.set(tenant); return Mono.just("billed"); });
        StepVerifier.create(P07TenantContext.withTenant(call, "globex")).expectNext("billed").verifyComplete();
        assertThat(header.get()).isEqualTo("globex");
    }

    @Test
    void placementMatters_contextOnlyFlowsUpstream() {
        Mono<String> wrong = Mono.just("c-1")
                .contextWrite(Context.of(P07TenantContext.TENANT, "acme"))   // written ABOVE the reader...
                .flatMap(P07TenantContext::loadInvoices);                    // ...so the reader below can't see it
        StepVerifier.create(wrong).expectNext("SELECT * FROM UNKNOWN.invoices WHERE customer='c-1'").verifyComplete();
    }

    @Test
    void innerWriteOverridesOuter_forItsSubtreeOnly() {
        Mono<String> inner = P07TenantContext.loadInvoices("c-9").contextWrite(Context.of(P07TenantContext.TENANT, "override"));
        StepVerifier.create(P07TenantContext.withTenant(inner, "acme")).expectNext("SELECT * FROM override.invoices WHERE customer='c-9'").verifyComplete();
    }
}
