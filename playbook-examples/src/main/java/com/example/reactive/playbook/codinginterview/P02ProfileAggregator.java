package com.example.reactive.playbook.codinginterview;

import java.time.Duration;
import java.util.List;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/** Problem 2: fan out to 3 services with WebClient in parallel; required vs optional calls; timeouts + fallbacks. */
public class P02ProfileAggregator {

    public record User(String id, String name) { }
    public record Order(String id, long cents) { }
    public record Profile(User user, List<Order> recentOrders, int loyaltyPoints, List<String> degraded) { }

    private final WebClient http;
    private final Duration optionalTimeout;

    public P02ProfileAggregator(WebClient http, Duration optionalTimeout) { this.http = http; this.optionalTimeout = optionalTimeout; }

    public Mono<Profile> profile(String userId) {
        Mono<User> user = http.get().uri("/users/{id}", userId).retrieve().bodyToMono(User.class);   // required: errors propagate

        Mono<Result<List<Order>>> orders = http.get().uri("/orders?user={id}", userId).retrieve()
                .bodyToFlux(Order.class).take(3).collectList()
                .timeout(optionalTimeout)
                .map(Result::ok)
                .onErrorResume(e -> Mono.just(Result.fallback(List.of(), "orders: " + reason(e))));

        Mono<Result<Integer>> points = http.get().uri("/loyalty/{id}", userId).retrieve().bodyToMono(Integer.class)
                .timeout(optionalTimeout)
                .map(Result::ok)
                .onErrorResume(e -> Mono.just(Result.fallback(0, "loyalty: " + reason(e))));

        // zip subscribes to all three at once: latency = slowest call, not the sum
        return Mono.zip(user, orders, points).map(t -> new Profile(t.getT1(), t.getT2().value(), t.getT3().value(),
                java.util.stream.Stream.of(t.getT2().degraded(), t.getT3().degraded()).filter(java.util.Objects::nonNull).toList()));
    }

    private static String reason(Throwable e) {
        if (e instanceof java.util.concurrent.TimeoutException) return "timeout";
        if (e instanceof WebClientResponseException w) return String.valueOf(w.getStatusCode().value());
        return e.getClass().getSimpleName();
    }

    record Result<T>(T value, String degraded) {
        static <T> Result<T> ok(T v) { return new Result<>(v, null); }
        static <T> Result<T> fallback(T v, String why) { return new Result<>(v, why); }
    }
}
