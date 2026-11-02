package com.example.reactive.playbook.web;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// --8<-- [start:controller]
@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductRepository repository;

    public ProductController(ProductRepository repository) {   // constructor injection
        this.repository = repository;
    }

    /** Flux return type → streamed JSON array (or NDJSON/SSE if the client asks for it). */
    @GetMapping
    public Flux<Product> list(@RequestParam(required = false) String category) {
        return category == null ? repository.findAll() : repository.findByCategory(category);
    }

    /** Empty Mono → error → mapped to 404 by the @RestControllerAdvice. */
    @GetMapping("/{id}")
    public Mono<Product> get(@PathVariable String id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(() -> new ProductNotFoundException(id)));
    }

    /** @Valid on a reactive body: validation runs when the body is decoded. */
    @PostMapping
    public Mono<ResponseEntity<Product>> create(@Valid @RequestBody Mono<Product> body) {
        return body.flatMap(repository::save)
                .map(saved -> ResponseEntity.created(URI.create("/products/" + saved.id())).body(saved));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        return repository.deleteById(id)
                .map(deleted -> deleted
                        ? ResponseEntity.noContent().<Void>build()
                        : ResponseEntity.notFound().<Void>build());
    }
}
// --8<-- [end:controller]
