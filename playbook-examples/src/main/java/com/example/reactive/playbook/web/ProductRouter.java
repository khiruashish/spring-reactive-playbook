package com.example.reactive.playbook.web;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/** The same API as {@link ProductController}, expressed as functional endpoints under /v2. */
@Configuration
public class ProductRouter {

    // --8<-- [start:handler]
    static final class ProductHandler {
        private final ProductRepository repository;

        ProductHandler(ProductRepository repository) {
            this.repository = repository;
        }

        Mono<ServerResponse> list(ServerRequest request) {
            return ServerResponse.ok().body(repository.findAll(), Product.class);
        }

        Mono<ServerResponse> get(ServerRequest request) {
            return repository.findById(request.pathVariable("id"))
                    .flatMap(p -> ServerResponse.ok().bodyValue(p))
                    .switchIfEmpty(ServerResponse.status(HttpStatus.NOT_FOUND)
                            .bodyValue(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "no such product")));
        }

        Mono<ServerResponse> create(ServerRequest request) {
            return request.bodyToMono(Product.class)
                    .flatMap(repository::save)
                    .flatMap(p -> ServerResponse.created(URI.create("/v2/products/" + p.id())).bodyValue(p));
        }
    }
    // --8<-- [end:handler]

    // --8<-- [start:router]
    @Bean
    public RouterFunction<ServerResponse> productRoutes(ProductRepository repository) {
        ProductHandler handler = new ProductHandler(repository);
        return RouterFunctions.route()
                .path("/v2/products", builder -> builder
                        .GET("", accept(MediaType.APPLICATION_JSON), handler::list)
                        .GET("/{id}", handler::get)
                        .POST("", handler::create))
                .filter(apiKeyRequiredForWrites())
                .build();
    }

    /** HandlerFilterFunction: like a WebFilter, but scoped to these routes only. */
    static HandlerFilterFunction<ServerResponse, ServerResponse> apiKeyRequiredForWrites() {
        return (request, next) -> request.method().name().equals("GET")
                || "secret".equals(request.headers().firstHeader("X-Api-Key"))
                ? next.handle(request)
                : ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
    }
    // --8<-- [end:router]
}
