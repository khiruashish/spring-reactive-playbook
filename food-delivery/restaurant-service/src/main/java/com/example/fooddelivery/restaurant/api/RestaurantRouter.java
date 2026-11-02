package com.example.fooddelivery.restaurant.api;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/** Functional endpoint routing: routes are plain data, composed and tested without a server. */
@Configuration
public class RestaurantRouter {

    // --8<-- [start:router]
    @Bean
    public RouterFunction<ServerResponse> restaurantRoutes(RestaurantHandler handler) {
        return RouterFunctions.route()
                .path("/api", api -> api
                        .nest(accept(MediaType.APPLICATION_JSON), json -> json
                                .GET("/restaurants", handler::list)
                                .POST("/restaurants", handler::create)
                                .GET("/restaurants/{id}", handler::get)
                                .GET("/restaurants/{id}/menu", handler::menu)
                                .POST("/restaurants/{id}/menu", handler::addMenuItem)
                                .GET("/menu-items", handler::menuItemsByIds)))
                .onError(Throwable.class, RestaurantHandler::problem)
                .build();
    }
    // --8<-- [end:router]
}
