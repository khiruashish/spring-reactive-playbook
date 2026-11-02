package com.example.fooddelivery.order.domain;

import com.example.fooddelivery.events.OrderPlaced;
import java.util.Collection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Outbound ports. Application code depends on these interfaces; infrastructure implements them. */
public final class Ports {

    public interface OrderStore {
        Mono<Order> insert(Order order);

        Mono<Order> findById(long id);

        Mono<Order> findByIdempotencyKey(String key);

        Flux<Order> findByCustomer(String customerId);

        Mono<Void> updateStatus(long id, OrderStatus status, String paymentId);
    }

    public interface MenuCatalog {
        Flux<MenuItemQuote> quotes(Collection<Long> menuItemIds);
    }

    public interface Payments {
        enum Outcome { CAPTURED, DECLINED }

        record Receipt(String paymentId, Outcome outcome) {
        }

        Mono<Receipt> charge(String idempotencyKey, String orderRef, String customerId, long amountPaise);
    }

    public interface OrderEvents {
        Mono<Void> orderPlaced(OrderPlaced event);
    }

    private Ports() {
    }
}
