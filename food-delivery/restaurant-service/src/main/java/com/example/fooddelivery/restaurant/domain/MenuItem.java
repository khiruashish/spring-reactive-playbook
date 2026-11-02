package com.example.fooddelivery.restaurant.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** Price is held in minor units (paise) as a {@code long} — never floating point for money. */
@Table("menu_items")
public record MenuItem(@Id Long id, Long restaurantId, String name, String description,
                       long pricePaise, boolean available) {
}
