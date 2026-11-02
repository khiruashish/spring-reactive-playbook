package com.example.fooddelivery.restaurant.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** Immutable R2DBC entity: Spring Data instantiates records through their canonical constructor. */
@Table("restaurants")
public record Restaurant(@Id Long id, String name, String cuisine, String address, boolean open) {
}
