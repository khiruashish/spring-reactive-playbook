package com.example.fooddelivery.restaurant.api;

import com.example.fooddelivery.restaurant.api.RestaurantRequests.CreateMenuItem;
import com.example.fooddelivery.restaurant.api.RestaurantRequests.CreateRestaurant;
import com.example.fooddelivery.restaurant.domain.MenuItem;
import com.example.fooddelivery.restaurant.domain.MenuItemRepository;
import com.example.fooddelivery.restaurant.domain.Restaurant;
import com.example.fooddelivery.restaurant.domain.RestaurantRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Handler functions for the functional (router-based) WebFlux style.
 * Each method takes a {@link ServerRequest} and returns {@code Mono<ServerResponse>} — nothing blocks.
 */
@Component
public class RestaurantHandler {

    private final RestaurantRepository restaurants;
    private final MenuItemRepository menuItems;
    private final Validator validator;

    public RestaurantHandler(RestaurantRepository restaurants, MenuItemRepository menuItems, Validator validator) {
        this.restaurants = restaurants;
        this.menuItems = menuItems;
        this.validator = validator;
    }

    // --8<-- [start:list]
    public Mono<ServerResponse> list(ServerRequest request) {
        Flux<Restaurant> result = request.queryParam("cuisine")
                .map(restaurants::findByCuisineIgnoreCase)
                .orElseGet(restaurants::findAll);
        return ServerResponse.ok().body(result, Restaurant.class);
    }
    // --8<-- [end:list]

    public Mono<ServerResponse> get(ServerRequest request) {
        return restaurants.findById(id(request))
                .switchIfEmpty(Mono.error(notFound("restaurant", id(request))))
                .flatMap(r -> ServerResponse.ok().bodyValue(r));
    }

    // --8<-- [start:create]
    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(CreateRestaurant.class)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "body required")))
                .map(this::validate)
                .map(body -> new Restaurant(null, body.name(), body.cuisine(), body.address(), true))
                .flatMap(restaurants::save)
                .flatMap(saved -> ServerResponse
                        .created(URI.create("/api/restaurants/" + saved.id()))
                        .bodyValue(saved));
    }
    // --8<-- [end:create]

    public Mono<ServerResponse> menu(ServerRequest request) {
        long restaurantId = id(request);
        return restaurants.existsById(restaurantId)
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(notFound("restaurant", restaurantId)))
                .flatMap(exists -> ServerResponse.ok()
                        .body(menuItems.findByRestaurantIdOrderByName(restaurantId), MenuItem.class));
    }

    public Mono<ServerResponse> addMenuItem(ServerRequest request) {
        long restaurantId = id(request);
        return restaurants.findById(restaurantId)
                .switchIfEmpty(Mono.error(notFound("restaurant", restaurantId)))
                .then(request.bodyToMono(CreateMenuItem.class))
                .map(this::validate)
                .map(body -> new MenuItem(null, restaurantId, body.name(), body.description(), body.pricePaise(), true))
                .flatMap(menuItems::save)
                .flatMap(saved -> ServerResponse.created(URI.create("/api/menu-items/" + saved.id())).bodyValue(saved));
    }

    /**
     * Batch lookup used by order-service to price a cart. Accepts both {@code ?ids=1,2,3} and the
     * repeated form {@code ?ids=1&ids=2} that HTTP interface clients send for a collection.
     */
    public Mono<ServerResponse> menuItemsByIds(ServerRequest request) {
        List<Long> ids = request.queryParams().getOrDefault("ids", List.of()).stream()
                .flatMap(csv -> Arrays.stream(csv.split(",")))
                .filter(s -> !s.isBlank())
                .map(String::trim)
                .map(Long::valueOf)
                .toList();
        return ServerResponse.ok().body(menuItems.findAllById(ids), MenuItem.class);
    }

    private <T> T validate(T body) {
        Set<ConstraintViolation<T>> violations = validator.validate(body);
        if (!violations.isEmpty()) {
            String detail = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, detail);
        }
        return body;
    }

    private static long id(ServerRequest request) {
        try {
            return Long.parseLong(request.pathVariable("id"));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id must be numeric");
        }
    }

    private static ResponseStatusException notFound(String what, long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, what + " " + id + " not found");
    }

    /** Converts any error into an RFC 9457 problem response — used by the router's onError filter. */
    public static Mono<ServerResponse> problem(Throwable error, ServerRequest request) {
        HttpStatus status = error instanceof ResponseStatusException rse
                ? HttpStatus.valueOf(rse.getStatusCode().value())
                : HttpStatus.INTERNAL_SERVER_ERROR;
        String detail = error instanceof ResponseStatusException rse ? rse.getReason() : "unexpected error";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setInstance(request.uri());
        return ServerResponse.status(status).bodyValue(problem);
    }
}
