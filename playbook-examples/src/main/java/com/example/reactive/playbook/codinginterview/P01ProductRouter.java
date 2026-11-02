package com.example.reactive.playbook.codinginterview;

import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/** Problem 1: CRUD with FUNCTIONAL endpoints: explicit routing, manual validation, 201/404/400 as ProblemDetail. */
public class P01ProductRouter {

    public record CreateProduct(@NotBlank String name, @Positive long priceCents) { }
    public record Product(long id, String name, long priceCents) { }

    private final Map<Long, Product> store = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();
    private final Validator validator;

    public P01ProductRouter(Validator validator) { this.validator = validator; }

    public RouterFunction<ServerResponse> routes() {
        return route()
                .path("/products", b -> b
                        .GET("/{id}", this::get)
                        .POST("", this::create)
                        .DELETE("/{id}", this::delete))
                .build();
    }

    Mono<ServerResponse> create(ServerRequest req) {
        return req.bodyToMono(CreateProduct.class)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("request body is required")))
                .flatMap(this::validate)                                       // validation is explicit in functional style
                .map(c -> { Product p = new Product(ids.incrementAndGet(), c.name(), c.priceCents()); store.put(p.id(), p); return p; })
                .flatMap(p -> ServerResponse.created(URI.create("/products/" + p.id())).bodyValue(p))
                .onErrorResume(IllegalArgumentException.class, e -> problem(HttpStatus.BAD_REQUEST, e.getMessage()));
    }

    Mono<ServerResponse> get(ServerRequest req) {
        long id = Long.parseLong(req.pathVariable("id"));
        return Mono.justOrEmpty(store.get(id))
                .flatMap(p -> ServerResponse.ok().bodyValue(p))
                .switchIfEmpty(Mono.defer(() -> problem(HttpStatus.NOT_FOUND, "product " + id + " not found")));  // defer: build lazily
    }

    Mono<ServerResponse> delete(ServerRequest req) {
        store.remove(Long.parseLong(req.pathVariable("id")));
        return ServerResponse.noContent().build();
    }

    private Mono<CreateProduct> validate(CreateProduct c) {
        var violations = validator.validate(c);
        if (violations.isEmpty()) return Mono.just(c);
        String msg = violations.stream().map(v -> v.getPropertyPath() + " " + v.getMessage()).sorted().collect(Collectors.joining("; "));
        return Mono.error(new IllegalArgumentException(msg));
    }

    private static Mono<ServerResponse> problem(HttpStatus status, String detail) {
        return ServerResponse.status(status).bodyValue(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
