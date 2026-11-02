package com.example.reactive.playbook.web;

import java.time.Duration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

// --8<-- [start:webclient]
/** A resilient downstream call with WebClient: status mapping, timeout, retry, fallback. */
public class InventoryClient {

    public record Stock(String productId, int available) {
    }

    public static class InventoryUnavailableException extends RuntimeException {
        public InventoryUnavailableException(String message) {
            super(message);
        }
    }

    private final WebClient webClient;

    public InventoryClient(WebClient webClient) {     // built from the auto-configured WebClient.Builder
        this.webClient = webClient;
    }

    public Mono<Stock> stock(String productId) {
        return webClient.get()
                .uri("/inventory/{id}", productId)
                .retrieve()
                .onStatus(status -> status.value() == 404, resp -> Mono.empty())   // 404 → empty Mono
                .onStatus(HttpStatusCode::is4xxClientError, resp -> resp.createException())
                .bodyToMono(Stock.class)
                .timeout(Duration.ofSeconds(2))                                    // per attempt
                .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                        .filter(InventoryClient::isTransient)
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure())) // surface the real cause
                .onErrorMap(InventoryClient::isTransient,
                        e -> new InventoryUnavailableException("inventory down: " + e.getMessage()));
    }

    private static boolean isTransient(Throwable e) {
        return e instanceof WebClientResponseException w && w.getStatusCode().is5xxServerError()
                || e instanceof java.util.concurrent.TimeoutException;
    }
}
// --8<-- [end:webclient]
