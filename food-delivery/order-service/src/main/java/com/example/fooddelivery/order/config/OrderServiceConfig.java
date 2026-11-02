package com.example.fooddelivery.order.config;

import com.example.fooddelivery.order.infrastructure.RestaurantApi;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class OrderServiceConfig {

    @ConfigurationProperties("clients")
    public record ClientProperties(String restaurantUrl, String paymentUrl) {
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /** WebClient.Builder is a prototype-scoped, pre-configured bean (codecs, observation) — clone per target. */
    @Bean
    WebClient paymentClient(WebClient.Builder builder, ClientProperties props) {
        return builder.clone().baseUrl(props.paymentUrl()).build();
    }

    @Bean
    RestaurantApi restaurantApi(WebClient.Builder builder, ClientProperties props) {
        WebClient client = builder.clone().baseUrl(props.restaurantUrl()).build();
        return HttpServiceProxyFactory.builderFor(WebClientAdapter.create(client))
                .build()
                .createClient(RestaurantApi.class);
    }

    // --8<-- [start:breaker]
    /** Circuit breaker: opens at 50% failures over the last 10 calls, half-opens after 10s. */
    @Bean
    Customizer<ReactiveResilience4JCircuitBreakerFactory> paymentBreaker() {
        return factory -> factory.configure(builder -> builder
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .permittedNumberOfCallsInHalfOpenState(2)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(5)).build()),
                "payment");
    }
    // --8<-- [end:breaker]
}
