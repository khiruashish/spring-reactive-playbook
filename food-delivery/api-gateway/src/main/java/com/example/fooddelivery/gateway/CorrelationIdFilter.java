package com.example.fooddelivery.gateway;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Ensures every request carries an X-Correlation-Id and logs method, path, status and latency. */
@Component
class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);
    static final String HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String id = exchange.getRequest().getHeaders().getFirst(HEADER);
        String correlationId = id != null ? id : UUID.randomUUID().toString();
        ServerWebExchange mutated = exchange.mutate().request(r -> r.header(HEADER, correlationId)).build();
        mutated.getResponse().getHeaders().set(HEADER, correlationId);
        long start = System.nanoTime();
        return chain.filter(mutated)
                .doFinally(signal -> log.info("{} {} -> {} in {} ms [{}]",
                        exchange.getRequest().getMethod(), exchange.getRequest().getPath(),
                        mutated.getResponse().getStatusCode(), (System.nanoTime() - start) / 1_000_000, correlationId));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
