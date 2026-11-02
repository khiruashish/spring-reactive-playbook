package com.example.fooddelivery.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Edge identity propagation: strips any client-supplied {@code X-User-Id} (so it cannot be spoofed) and
 * replaces it with the authenticated JWT subject. The security context is read from the Reactor
 * {@code Context} via {@link ReactiveSecurityContextHolder} — there is no ThreadLocal in WebFlux.
 */
@Component
class UserIdRelayFilter implements GlobalFilter, Ordered {

    static final String USER_HEADER = "X-User-Id";

    // --8<-- [start:filter]
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(JwtAuthenticationToken.class::isInstance)
                .map(auth -> ((JwtAuthenticationToken) auth).getToken().getSubject())
                .map(userId -> withUser(exchange, userId))
                .defaultIfEmpty(withUser(exchange, null))
                .flatMap(chain::filter);
    }
    // --8<-- [end:filter]

    private static ServerWebExchange withUser(ServerWebExchange exchange, String userId) {
        return exchange.mutate().request(r -> r.headers(h -> {
            h.remove(USER_HEADER);
            if (userId != null) {
                h.set(USER_HEADER, userId);
            }
        })).build();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
