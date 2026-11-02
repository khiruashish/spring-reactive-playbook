package com.example.fooddelivery.order.infrastructure;

import com.example.fooddelivery.order.domain.MenuItemQuote;
import java.util.Collection;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import reactor.core.publisher.Flux;

/** Declarative HTTP interface: Spring generates a WebClient-backed implementation at runtime. */
@HttpExchange("/api")
public interface RestaurantApi {

    @GetExchange("/menu-items")
    Flux<MenuItemQuote> menuItems(@RequestParam("ids") Collection<Long> ids);
}
