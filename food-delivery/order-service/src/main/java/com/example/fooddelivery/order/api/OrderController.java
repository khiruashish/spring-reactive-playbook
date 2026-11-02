package com.example.fooddelivery.order.api;

import com.example.fooddelivery.order.api.OrderDtos.OrderResponse;
import com.example.fooddelivery.order.api.OrderDtos.PlaceOrderRequest;
import com.example.fooddelivery.order.application.OrderService;
import com.example.fooddelivery.order.application.OrderService.PlaceOrder;
import com.example.fooddelivery.order.application.OrderStatusUpdate;
import com.example.fooddelivery.order.domain.Pricing.CartLine;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Annotated-controller style. The gateway authenticates the caller and forwards their id as X-User-Id. */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    public Mono<ResponseEntity<OrderResponse>> place(@RequestHeader("X-User-Id") String customerId,
                                                     @RequestHeader(name = "Idempotency-Key", required = false) String key,
                                                     @Valid @RequestBody PlaceOrderRequest body) {
        var command = new PlaceOrder(customerId, body.restaurantId(), body.deliveryAddress(),
                body.items().stream().map(i -> new CartLine(i.menuItemId(), i.quantity())).toList());
        return orders.place(key, command)
                .map(order -> ResponseEntity.created(URI.create("/api/orders/" + order.id()))
                        .body(OrderResponse.from(order)));
    }

    @GetMapping("/{id}")
    public Mono<OrderResponse> get(@PathVariable long id) {
        return orders.get(id).map(OrderResponse::from);
    }

    @GetMapping
    public Flux<OrderResponse> mine(@RequestHeader("X-User-Id") String customerId) {
        return orders.forCustomer(customerId).map(OrderResponse::from);
    }

    // --8<-- [start:sse]
    /** Server-Sent Events: the browser's EventSource keeps one HTTP connection open and receives pushes. */
    @GetMapping(path = "/{id}/status-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<OrderStatusUpdate>> statusStream(@PathVariable long id) {
        Flux<ServerSentEvent<OrderStatusUpdate>> updates = orders.watch(id)
                .map(u -> ServerSentEvent.builder(u).event("status").id(u.status() + "-" + u.at()).build());
        // heartbeat comments keep proxies from closing an idle connection; stop them when updates complete
        Flux<ServerSentEvent<OrderStatusUpdate>> heartbeat = Flux.interval(Duration.ofSeconds(15))
                .map(t -> ServerSentEvent.<OrderStatusUpdate>builder().comment("keep-alive").build());
        return Flux.merge(updates, heartbeat.takeUntilOther(updates.ignoreElements().then(Mono.just(1))));
    }
    // --8<-- [end:sse]
}
