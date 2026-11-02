package com.example.fooddelivery.order.domain;

/** A menu item as priced by restaurant-service at the moment of ordering. */
public record MenuItemQuote(Long id, Long restaurantId, String name, long pricePaise, boolean available) {
}
