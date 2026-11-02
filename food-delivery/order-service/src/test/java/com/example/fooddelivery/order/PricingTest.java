package com.example.fooddelivery.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.fooddelivery.order.domain.MenuItemQuote;
import com.example.fooddelivery.order.domain.OrderStatus;
import com.example.fooddelivery.order.domain.Pricing;
import com.example.fooddelivery.order.domain.Pricing.CartLine;
import com.example.fooddelivery.order.domain.Pricing.InvalidCartException;
import java.util.List;
import org.junit.jupiter.api.Test;

class PricingTest {

    private final List<MenuItemQuote> menu = List.of(
            new MenuItemQuote(1L, 1L, "Paneer", 24900, true),
            new MenuItemQuote(2L, 1L, "Naan", 5900, true),
            new MenuItemQuote(9L, 2L, "Pizza", 34900, true),
            new MenuItemQuote(3L, 1L, "Sold out", 100, false));

    @Test
    void totalsLinesInPaise() {
        var priced = Pricing.price(1L, List.of(new CartLine(1L, 2), new CartLine(2L, 3)), menu);
        assertThat(priced.totalPaise()).isEqualTo(2 * 24900 + 3 * 5900);
        assertThat(priced.lines()).hasSize(2);
    }

    @Test
    void rejectsItemsFromAnotherRestaurant() {
        assertThatThrownBy(() -> Pricing.price(1L, List.of(new CartLine(9L, 1)), menu))
                .isInstanceOf(InvalidCartException.class).hasMessageContaining("does not belong");
    }

    @Test
    void rejectsUnknownAndUnavailableItems() {
        assertThatThrownBy(() -> Pricing.price(1L, List.of(new CartLine(77L, 1)), menu))
                .hasMessageContaining("unknown");
        assertThatThrownBy(() -> Pricing.price(1L, List.of(new CartLine(3L, 1)), menu))
                .hasMessageContaining("unavailable");
    }

    @Test
    void statusMachineRejectsRegressions() {
        assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.COURIER_ASSIGNED)).isTrue();
        assertThat(OrderStatus.IN_TRANSIT.canTransitionTo(OrderStatus.PICKED_UP)).isFalse();
        assertThat(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.IN_TRANSIT)).isFalse();
    }
}
