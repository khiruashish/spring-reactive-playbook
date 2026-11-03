package com.example.reactive.playbook.codinginterview;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** WebClient with a stubbed ExchangeFunction: real request/response/decoding, no network, no mock server. */
class P02ProfileAggregatorTest {

    static WebClient stub(Map<String, Function<String, Mono<ClientResponse>>> routes) {
        return WebClient.builder().exchangeFunction(req -> {
            String path = req.url().getPath();
            return routes.entrySet().stream().filter(e -> path.startsWith(e.getKey())).findFirst()
                    .map(e -> e.getValue().apply(path)).orElse(Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build()));
        }).build();
    }

    static Mono<ClientResponse> json(String body, long delayMs) {
        return Mono.delay(Duration.ofMillis(delayMs)).map(x -> ClientResponse.create(HttpStatus.OK).header("Content-Type", "application/json").body(body).build());
    }

    @Test
    void allHealthy_returnsFullProfile_inParallel() {
        var agg = new P02ProfileAggregator(stub(Map.of(
                "/users", p -> json("{\"id\":\"u1\",\"name\":\"Ana\"}", 100),
                "/orders", p -> json("[{\"id\":\"o1\",\"cents\":500},{\"id\":\"o2\",\"cents\":700}]", 100),
                "/loyalty", p -> json("120", 100))), Duration.ofSeconds(1));
        long start = System.nanoTime();
        StepVerifier.create(agg.profile("u1"))
                .assertNext(p -> {
                    assertThat(p.user().name()).isEqualTo("Ana");
                    assertThat(p.recentOrders()).hasSize(2);
                    assertThat(p.loyaltyPoints()).isEqualTo(120);
                    assertThat(p.degraded()).isEmpty();
                }).verifyComplete();
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(290));   // ~100 ms, not 300
    }

    @Test
    void slowAndFailingOptionalServices_degradeGracefully() {
        var agg = new P02ProfileAggregator(stub(Map.of(
                "/users", p -> json("{\"id\":\"u1\",\"name\":\"Ana\"}", 10),
                "/orders", p -> json("[]", 5_000),
                "/loyalty", p -> Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE).build()))), Duration.ofMillis(200));
        StepVerifier.create(agg.profile("u1"))
                .assertNext(p -> {
                    assertThat(p.recentOrders()).isEmpty();
                    assertThat(p.loyaltyPoints()).isZero();
                    assertThat(p.degraded()).containsExactly("orders: timeout", "loyalty: 503");
                }).verifyComplete();
    }

    @Test
    void requiredServiceFailure_failsTheWholeProfile() {
        var agg = new P02ProfileAggregator(stub(Map.of(
                "/orders", p -> json("[]", 10), "/loyalty", p -> json("1", 10))), Duration.ofSeconds(1));   // /users -> 404
        StepVerifier.create(agg.profile("nobody")).expectError(WebClientResponseException.NotFound.class).verify();
    }
}
