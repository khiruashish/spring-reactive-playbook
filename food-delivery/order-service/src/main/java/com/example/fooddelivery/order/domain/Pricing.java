package com.example.fooddelivery.order.domain;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Pure pricing rules — no Reactor, no Spring: unit-testable in microseconds. */
public final class Pricing {

    public record CartLine(Long menuItemId, int quantity) {
    }

    public record PricedCart(Long restaurantId, List<OrderLine> lines, long totalPaise) {
    }

    public static final class InvalidCartException extends RuntimeException {
        public InvalidCartException(String message) {
            super(message);
        }
    }

    private Pricing() {
    }

    public static PricedCart price(Long restaurantId, List<CartLine> cart, List<MenuItemQuote> quotes) {
        if (cart.isEmpty()) {
            throw new InvalidCartException("cart is empty");
        }
        Map<Long, MenuItemQuote> byId = quotes.stream()
                .collect(Collectors.toMap(MenuItemQuote::id, Function.identity()));
        List<OrderLine> lines = cart.stream().map(line -> {
            if (line.quantity() <= 0) {
                throw new InvalidCartException("quantity must be positive for item " + line.menuItemId());
            }
            MenuItemQuote quote = byId.get(line.menuItemId());
            if (quote == null) {
                throw new InvalidCartException("unknown menu item " + line.menuItemId());
            }
            if (!quote.restaurantId().equals(restaurantId)) {
                throw new InvalidCartException("item " + quote.id() + " does not belong to restaurant " + restaurantId);
            }
            if (!quote.available()) {
                throw new InvalidCartException("item " + quote.name() + " is unavailable");
            }
            return new OrderLine(quote.id(), quote.name(), line.quantity(), quote.pricePaise());
        }).toList();
        long total = lines.stream().mapToLong(OrderLine::lineTotalPaise).reduce(0L, Math::addExact);
        return new PricedCart(restaurantId, lines, total);
    }
}
