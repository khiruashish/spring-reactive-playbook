package com.example.fooddelivery.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.fooddelivery.events.OrderPlaced;
import com.example.fooddelivery.order.application.OrderService;
import com.example.fooddelivery.order.application.OrderService.PlaceOrder;
import com.example.fooddelivery.order.application.OrderStatusStream;
import com.example.fooddelivery.order.application.OrderStatusUpdate;
import com.example.fooddelivery.order.domain.MenuItemQuote;
import com.example.fooddelivery.order.domain.Order;
import com.example.fooddelivery.order.domain.OrderStatus;
import com.example.fooddelivery.order.domain.Ports.MenuCatalog;
import com.example.fooddelivery.order.domain.Ports.OrderStore;
import com.example.fooddelivery.order.domain.Ports.Payments;
import com.example.fooddelivery.order.domain.Pricing.CartLine;
import com.example.fooddelivery.order.domain.Pricing.InvalidCartException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** The whole place-order pipeline against in-memory port implementations — no Spring, no Docker. */
class OrderServiceTest {

    private final InMemoryStore store = new InMemoryStore();
    private final List<OrderPlaced> published = new CopyOnWriteArrayList<>();
    private final AtomicInteger charges = new AtomicInteger();
    private final OrderStatusStream stream = new OrderStatusStream();

    private final MenuCatalog catalog = ids -> Flux.just(
            new MenuItemQuote(1L, 1L, "Paneer", 24900, true),
            new MenuItemQuote(2L, 1L, "Naan", 5900, true));

    private OrderService service(Payments payments) {
        return new OrderService(store, catalog, payments, e -> Mono.fromRunnable(() -> published.add(e)), stream,
                Clock.fixed(Instant.parse("2026-11-02T10:00:00Z"), ZoneOffset.UTC));
    }

    private Payments capturing() {
        return (key, ref, customer, amount) -> {
            charges.incrementAndGet();
            return Mono.just(new Payments.Receipt("pay-1", Payments.Outcome.CAPTURED));
        };
    }

    private static PlaceOrder order() {
        return new PlaceOrder("alice", 1L, "221B Baker St", List.of(new CartLine(1L, 2), new CartLine(2L, 1)));
    }

    @Test
    void placesPaysAndPublishes() {
        StepVerifier.create(service(capturing()).place("k-1", order()))
                .assertNext(o -> {
                    assertThat(o.status()).isEqualTo(OrderStatus.PAID);
                    assertThat(o.totalPaise()).isEqualTo(2 * 24900 + 5900);
                })
                .verifyComplete();

        assertThat(published).singleElement().satisfies(e -> assertThat(e.totalPaise()).isEqualTo(55700));
    }

    @Test
    void sameIdempotencyKeyDoesNotChargeTwice() {
        OrderService svc = service(capturing());
        Order first = svc.place("k-1", order()).block();
        Order replay = svc.place("k-1", order()).block();

        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(charges).hasValue(1);
    }

    @Test
    void paymentOutageMarksOrderFailedAndPublishesNothing() {
        Payments down = (k, r, c, a) -> Mono.error(new IllegalStateException("payment unavailable"));

        StepVerifier.create(service(down).place("k-2", order()))
                .assertNext(o -> assertThat(o.status()).isEqualTo(OrderStatus.PAYMENT_FAILED))
                .verifyComplete();
        assertThat(published).isEmpty();
    }

    @Test
    void invalidCartErrorsBeforeTouchingTheDatabase() {
        var bad = new PlaceOrder("alice", 2L, "x", List.of(new CartLine(1L, 1)));
        StepVerifier.create(service(capturing()).place("k-3", bad))
                .expectError(InvalidCartException.class)
                .verify();
        assertThat(store.orders).isEmpty();
    }

    @Test
    void watchEmitsSnapshotThenLiveUpdatesUntilDelivered() {
        OrderService svc = service(capturing());
        long id = svc.place("k-4", order()).block().id();
        Instant now = Instant.now();

        StepVerifier.create(svc.watch(id).map(OrderStatusUpdate::status))
                .expectNext(OrderStatus.PAID)
                .then(() -> svc.applyDeliveryUpdate(id, OrderStatus.COURIER_ASSIGNED, 0, 0, now).block())
                .expectNext(OrderStatus.COURIER_ASSIGNED)
                .then(() -> svc.applyDeliveryUpdate(id, OrderStatus.PAID, 0, 0, now).block()) // stale: ignored
                .then(() -> svc.applyDeliveryUpdate(id, OrderStatus.DELIVERED, 0, 0, now).block())
                .expectNext(OrderStatus.DELIVERED)
                .verifyComplete();
    }

    /** Minimal fake of the persistence port. */
    static final class InMemoryStore implements OrderStore {
        final Map<Long, Order> orders = new ConcurrentHashMap<>();
        private final AtomicLong ids = new AtomicLong();

        @Override
        public Mono<Order> insert(Order order) {
            return Mono.fromSupplier(() -> {
                Order saved = new Order(ids.incrementAndGet(), order.customerId(), order.restaurantId(),
                        order.status(), order.totalPaise(), order.deliveryAddress(), order.idempotencyKey(),
                        order.paymentId(), order.createdAt(), order.lines());
                orders.put(saved.id(), saved);
                return saved;
            });
        }

        @Override
        public Mono<Order> findById(long id) {
            return Mono.justOrEmpty(orders.get(id));
        }

        @Override
        public Mono<Order> findByIdempotencyKey(String key) {
            return Flux.fromIterable(orders.values()).filter(o -> key.equals(o.idempotencyKey())).next();
        }

        @Override
        public Flux<Order> findByCustomer(String customerId) {
            return Flux.fromIterable(orders.values()).filter(o -> o.customerId().equals(customerId));
        }

        @Override
        public Mono<Void> updateStatus(long id, OrderStatus status, String paymentId) {
            return Mono.fromRunnable(() -> orders.computeIfPresent(id, (k, o) -> o.withStatus(status, paymentId)));
        }
    }
}
