package com.example.fooddelivery.order.infrastructure;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("order_lines")
record OrderLineEntity(@Id Long id, Long orderId, Long menuItemId, String name, int quantity, long unitPricePaise) {
}
