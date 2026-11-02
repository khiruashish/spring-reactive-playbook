package com.example.reactive.playbook.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/** Generic example domain for the WebFlux chapters. Prices in minor units (cents/paise). */
public record Product(String id, @NotBlank String name, @NotBlank String category, @PositiveOrZero long priceMinor) {

    public Product withId(String newId) {
        return new Product(newId, name, category, priceMinor);
    }
}
