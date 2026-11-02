package com.example.fooddelivery.order.api;

import com.example.fooddelivery.order.domain.Order;
import com.example.fooddelivery.order.domain.OrderLine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    public record PlaceOrderRequest(
            @NotNull Long restaurantId,
            @NotBlank String deliveryAddress,
            @NotEmpty List<@Valid Item> items) {

        public record Item(@NotNull Long menuItemId, @Positive int quantity) {
        }
    }

    public record OrderResponse(Long id, String customerId, Long restaurantId, String status, long totalPaise,
                                String deliveryAddress, String paymentId, Instant createdAt, List<OrderLine> lines) {

        public static OrderResponse from(Order o) {
            return new OrderResponse(o.id(), o.customerId(), o.restaurantId(), o.status().name(), o.totalPaise(),
                    o.deliveryAddress(), o.paymentId(), o.createdAt(), o.lines());
        }
    }

    private OrderDtos() {
    }
}
