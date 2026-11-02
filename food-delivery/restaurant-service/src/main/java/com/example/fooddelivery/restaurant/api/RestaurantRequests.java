package com.example.fooddelivery.restaurant.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** Request bodies for the functional endpoints, validated programmatically in the handler. */
public final class RestaurantRequests {

    public record CreateRestaurant(@NotBlank String name, @NotBlank String cuisine, @NotBlank String address) {
    }

    public record CreateMenuItem(@NotBlank String name, String description, @Positive long pricePaise) {
    }

    private RestaurantRequests() {
    }
}
