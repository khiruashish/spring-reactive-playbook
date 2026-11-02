package com.example.fooddelivery.order.infrastructure;

import com.example.fooddelivery.order.domain.Order;
import com.example.fooddelivery.order.domain.OrderLine;
import com.example.fooddelivery.order.domain.OrderStatus;
import com.example.fooddelivery.order.domain.Ports.OrderStore;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** R2DBC adapter: maps the aggregate to two tables. R2DBC has no lazy relations, so lines are loaded explicitly. */
@Component
class R2dbcOrderStore implements OrderStore {

    private final OrderEntityRepository orders;
    private final OrderLineEntityRepository lines;

    R2dbcOrderStore(OrderEntityRepository orders, OrderLineEntityRepository lines) {
        this.orders = orders;
        this.lines = lines;
    }

    /** Reactive transaction: both inserts commit or roll back together (R2dbcTransactionManager). */
    @Override
    @Transactional
    public Mono<Order> insert(Order order) {
        var entity = new OrderEntity(null, order.customerId(), order.restaurantId(), order.status().name(),
                order.totalPaise(), order.deliveryAddress(), order.idempotencyKey(), order.paymentId(),
                order.createdAt(), order.createdAt());
        return orders.save(entity).flatMap(saved -> lines
                .saveAll(order.lines().stream()
                        .map(l -> new OrderLineEntity(null, saved.id(), l.menuItemId(), l.name(), l.quantity(),
                                l.unitPricePaise()))
                        .toList())
                .then(Mono.just(toDomain(saved).withLines(order.lines()))));
    }

    @Override
    public Mono<Order> findById(long id) {
        return orders.findById(id).flatMap(this::withLines);
    }

    @Override
    public Mono<Order> findByIdempotencyKey(String key) {
        return key == null ? Mono.empty() : orders.findByIdempotencyKey(key).flatMap(this::withLines);
    }

    @Override
    public Flux<Order> findByCustomer(String customerId) {
        // concatMap keeps newest-first ordering while loading each order's lines
        return orders.findByCustomerIdOrderByCreatedAtDesc(customerId).concatMap(this::withLines);
    }

    @Override
    public Mono<Void> updateStatus(long id, OrderStatus status, String paymentId) {
        return orders.updateStatus(id, status.name(), paymentId, Instant.now()).then();
    }

    private Mono<Order> withLines(OrderEntity entity) {
        return lines.findByOrderId(entity.id())
                .map(l -> new OrderLine(l.menuItemId(), l.name(), l.quantity(), l.unitPricePaise()))
                .collectList()
                .map(loaded -> toDomain(entity).withLines(loaded));
    }

    private static Order toDomain(OrderEntity e) {
        return new Order(e.id(), e.customerId(), e.restaurantId(), OrderStatus.valueOf(e.status()), e.totalPaise(),
                e.deliveryAddress(), e.idempotencyKey(), e.paymentId(), e.createdAt(), java.util.List.of());
    }
}
