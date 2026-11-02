package com.example.reactive.playbook.web;

import java.util.UUID;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

// --8<-- [start:webfilter]
/**
 * Runs for every request (annotated AND functional endpoints). Adds a request id to the response and
 * to the Reactor Context so any downstream operator can read it — the reactive replacement for MDC/ThreadLocal.
 */
@Component
@Order(-200)  // before Spring Security (-100) so even rejected requests get an id
public class RequestIdWebFilter implements WebFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String CONTEXT_KEY = "requestId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        String requestId = incoming != null ? incoming : UUID.randomUUID().toString();
        exchange.getResponse().getHeaders().set(HEADER, requestId);
        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(CONTEXT_KEY, requestId));
    }
}
// --8<-- [end:webfilter]
