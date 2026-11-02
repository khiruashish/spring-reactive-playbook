package com.example.fooddelivery.order.infrastructure;

import com.example.fooddelivery.order.domain.MenuItemQuote;
import com.example.fooddelivery.order.domain.Ports.MenuCatalog;
import java.time.Duration;
import java.util.Collection;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

@Component
class HttpMenuCatalog implements MenuCatalog {

    private final RestaurantApi api;

    HttpMenuCatalog(RestaurantApi api) {
        this.api = api;
    }

    @Override
    public Flux<MenuItemQuote> quotes(Collection<Long> menuItemIds) {
        return api.menuItems(menuItemIds)
                .timeout(Duration.ofSeconds(2))
                // a read is safe to retry: 2 retries, exponential backoff with jitter
                .retryWhen(Retry.backoff(2, Duration.ofMillis(100)).jitter(0.5));
    }
}
