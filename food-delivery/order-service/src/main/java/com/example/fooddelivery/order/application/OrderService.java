package com.example.fooddelivery.order.application;

import com.example.fooddelivery.events.OrderPlaced;
import com.example.fooddelivery.order.domain.Order;
import com.example.fooddelivery.order.domain.OrderNotFoundException;
import com.example.fooddelivery.order.domain.OrderStatus;
import com.example.fooddelivery.order.domain.Ports.MenuCatalog;
import com.example.fooddelivery.order.domain.Ports.OrderEvents;
import com.example.fooddelivery.order.domain.Ports.OrderStore;
import com.example.fooddelivery.order.domain.Ports.Payments;
import com.example.fooddelivery.order.domain.Pricing;
import com.example.fooddelivery.order.domain.Pricing.CartLine;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Place-order use case, written as one declarative pipeline:
 * idempotency check → price cart → persist → charge → mark paid/failed → publish event → notify watchers.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public record PlaceOrder(String customerId, Long restaurantId, String deliveryAddress, List<CartLine> items) {
    }

    private final OrderStore store;
    private final MenuCatalog catalog;
    private final Payments payments;
    private final OrderEvents events;
    private final OrderStatusStream statusStream;
    private final Clock clock;

    public OrderService(OrderStore store, MenuCatalog catalog, Payments payments, OrderEvents events,
                        OrderStatusStream statusStream, Clock clock) {
        this.store = store;
        this.catalog = catalog;
        this.payments = payments;
        this.events = events;
        this.statusStream = statusStream;
        this.clock = clock;
    }

    public Mono<Order> place(String idempotencyKey, PlaceOrder command) {
        // Replays of the same Idempotency-Key return the original order instead of charging twice.
        return store.findByIdempotencyKey(idempotencyKey)
                .doOnNext(existing -> log.info("idempotent replay key={} order={}", idempotencyKey, existing.id()))
                .switchIfEmpty(Mono.defer(() -> createAndPay(idempotencyKey, command)));
    }

    private Mono<Order> createAndPay(String idempotencyKey, PlaceOrder command) {
        List<Long> ids = command.items().stream().map(CartLine::menuItemId).distinct().toList();
        return catalog.quotes(ids)
                .collectList()
                .map(quotes -> Pricing.price(command.restaurantId(), command.items(), quotes))
                .map(priced -> new Order(null, command.customerId(), priced.restaurantId(),
                        OrderStatus.PENDING_PAYMENT, priced.totalPaise(), command.deliveryAddress(),
                        idempotencyKey, null, Instant.now(clock), priced.lines()))
                .flatMap(store::insert)
                .flatMap(this::charge);
    }

    private Mono<Order> charge(Order order) {
        return payments.charge(order.idempotencyKey(), "order-" + order.id(), order.customerId(), order.totalPaise())
                .map(receipt -> receipt.outcome() == Payments.Outcome.CAPTURED
                        ? order.withStatus(OrderStatus.PAID, receipt.paymentId())
                        : order.withStatus(OrderStatus.PAYMENT_FAILED, receipt.paymentId()))
                // the payment client already applied timeout + retry + circuit breaker; anything left is a failure
                .onErrorResume(e -> {
                    log.warn("payment failed for order {}: {}", order.id(), e.toString());
                    return Mono.just(order.withStatus(OrderStatus.PAYMENT_FAILED, null));
                })
                .flatMap(updated -> store.updateStatus(updated.id(), updated.status(), updated.paymentId())
                        .then(updated.status() == OrderStatus.PAID ? events.orderPlaced(toEvent(updated)) : Mono.empty())
                        .thenReturn(updated))
                .doOnNext(updated -> statusStream.publish(
                        new OrderStatusUpdate(updated.id(), updated.status(), null, null, Instant.now(clock))));
    }

    public Mono<Order> get(long id) {
        return store.findById(id).switchIfEmpty(Mono.error(() -> new OrderNotFoundException(id)));
    }

    public Flux<Order> forCustomer(String customerId) {
        return store.findByCustomer(customerId);
    }

    /** Applies a delivery-service status change, ignoring stale or out-of-order transitions. */
    public Mono<Void> applyDeliveryUpdate(long orderId, OrderStatus next, double lat, double lon, Instant at) {
        return store.findById(orderId)
                .filter(order -> order.status().canTransitionTo(next))
                .flatMap(order -> store.updateStatus(orderId, next, order.paymentId()).thenReturn(order))
                // doOnNext (not doOnSuccess): only fires when a transition was actually applied
                .doOnNext(order -> statusStream.publish(new OrderStatusUpdate(orderId, next, lat, lon, at)))
                .then();
    }

    /**
     * Current status first, then live updates, completing once the order reaches a terminal state.
     * (A transition landing between the snapshot read and the live subscription is only visible on the
     * next update — acceptable for a UI; use a replaying source such as Kafka offsets when it is not.)
     */
    public Flux<OrderStatusUpdate> watch(long orderId) {
        Mono<OrderStatusUpdate> current = get(orderId)
                .map(o -> new OrderStatusUpdate(o.id(), o.status(), null, null, Instant.now(clock)));
        return current.flatMapMany(snapshot -> snapshot.status().isTerminal()
                ? Flux.just(snapshot)
                : Flux.concat(Flux.just(snapshot), statusStream.updatesFor(orderId))
                        .takeUntil(u -> u.status().isTerminal()));
    }

    private static OrderPlaced toEvent(Order order) {
        return new OrderPlaced(
                String.valueOf(order.id()),
                order.customerId(),
                String.valueOf(order.restaurantId()),
                order.totalPaise(),
                order.deliveryAddress(),
                order.lines().stream()
                        .map(l -> new OrderPlaced.Line(String.valueOf(l.menuItemId()), l.name(), l.quantity(),
                                l.unitPricePaise()))
                        .toList(),
                order.createdAt());
    }
}
