package com.example.reactive.playbook.codinginterview;

import java.util.function.Function;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/** Problem 7: carry tenant id / correlation id through a reactive chain WITHOUT ThreadLocal (threads change per operator). */
public class P07TenantContext {

    public static final String TENANT = "tenant";

    /** Deep inside the chain: read the tenant from the subscriber Context. */
    public static Mono<String> loadInvoices(String customer) {
        return Mono.deferContextual(ctx -> {
            String tenant = ctx.getOrDefault(TENANT, "UNKNOWN");
            return Mono.just("SELECT * FROM " + tenant + ".invoices WHERE customer='" + customer + "'");
        });
    }

    /** A downstream HTTP call that forwards the tenant as a header, also read from Context. */
    public static Mono<String> callBilling(Function<String, Mono<String>> httpWithTenantHeader) {
        return Mono.deferContextual(ctx -> httpWithTenantHeader.apply(ctx.get(TENANT)));
    }

    /** At the edge (a WebFilter in a real app): write the Context ONCE; it flows upstream to every operator. */
    public static <T> Mono<T> withTenant(Mono<T> pipeline, String tenant) {
        return pipeline.contextWrite(Context.of(TENANT, tenant));
    }
}
