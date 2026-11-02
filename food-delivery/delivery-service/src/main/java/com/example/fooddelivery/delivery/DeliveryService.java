package com.example.fooddelivery.delivery;

import com.example.fooddelivery.events.DeliveryStatus;
import com.example.fooddelivery.events.DeliveryStatusChanged;
import com.example.fooddelivery.events.OrderPlaced;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);
    // a fixed "restaurant hub" and a pseudo-random drop point nearby (Bengaluru)
    private static final double HUB_LAT = 12.9716;
    private static final double HUB_LON = 77.5946;

    private final DeliveryRepository repository;
    private final DeliveryEvents events;
    private final SimulationProperties sim;
    /** Tracks running simulations so they are cancelled on shutdown instead of leaking. */
    private final Disposable.Composite running = Disposables.composite();

    public DeliveryService(DeliveryRepository repository, DeliveryEvents events, SimulationProperties sim) {
        this.repository = repository;
        this.events = events;
        this.sim = sim;
    }

    /** Assign a courier (idempotent per order), then start the trip in the background. */
    public Mono<Delivery> onOrderPlaced(OrderPlaced order) {
        String courier = sim.couriers().get(Math.floorMod(order.orderId().hashCode(), sim.couriers().size()));
        var delivery = new Delivery(null, order.orderId(), courier, DeliveryStatus.COURIER_ASSIGNED,
                HUB_LAT, HUB_LON, Instant.now());
        return repository.save(delivery)
                .flatMap(saved -> events.publish(toEvent(saved)).thenReturn(saved))
                .doOnNext(this::startTrip)
                // redelivered Kafka message → unique index on orderId rejects the duplicate: treat as done
                .onErrorResume(DuplicateKeyException.class, dup -> repository.findByOrderId(order.orderId()));
    }

    // --8<-- [start:trip]
    private void startTrip(Delivery assigned) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        double toLat = HUB_LAT + rnd.nextDouble(-0.05, 0.05);
        double toLon = HUB_LON + rnd.nextDouble(-0.05, 0.05);

        Disposable trip = CourierSimulator.route(HUB_LAT, HUB_LON, toLat, toLon, sim.steps(), sim.tick())
                // concatMap: strictly one waypoint at a time — save, then publish, then the next one
                .concatMap(wp -> repository.findById(assigned.id())
                        .map(d -> d.moveTo(wp.status(), wp.latitude(), wp.longitude(), Instant.now()))
                        .flatMap(repository::save)
                        .flatMap(saved -> events.publish(toEvent(saved))))
                .subscribe(
                        ignored -> { },
                        error -> log.error("trip for order {} failed", assigned.orderId(), error),
                        () -> log.info("order {} delivered by {}", assigned.orderId(), assigned.courierId()));
        running.add(trip);
    }
    // --8<-- [end:trip]

    public Mono<Delivery> forOrder(String orderId) {
        return repository.findByOrderId(orderId);
    }

    /** Last known position, then every live change for this order until it is delivered. */
    public Flux<DeliveryStatusChanged> track(String orderId) {
        Flux<DeliveryStatusChanged> live = events.live().filter(e -> e.orderId().equals(orderId));
        return forOrder(orderId).map(DeliveryService::toEvent)
                .flatMapMany(current -> current.status() == DeliveryStatus.DELIVERED
                        ? Flux.just(current)
                        : Flux.concat(Flux.just(current), live))
                .switchIfEmpty(live)  // order not yet picked up by delivery-service: wait for it
                .takeUntil(e -> e.status() == DeliveryStatus.DELIVERED);
    }

    @PreDestroy
    void stop() {
        running.dispose();
    }

    static DeliveryStatusChanged toEvent(Delivery d) {
        return new DeliveryStatusChanged(d.orderId(), d.id(), d.courierId(), d.status(), d.latitude(), d.longitude(),
                d.updatedAt());
    }
}
