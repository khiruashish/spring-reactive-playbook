package com.example.reactive.playbook.web;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** In-memory stand-in for a reactive repository (R2DBC/Mongo) so examples run without a database. */
@Repository
public class ProductRepository {

    private final Map<String, Product> store = new ConcurrentHashMap<>();

    public Flux<Product> findAll() {
        return Flux.fromIterable(store.values());
    }

    public Flux<Product> findByCategory(String category) {
        return findAll().filter(p -> p.category().equalsIgnoreCase(category));
    }

    public Mono<Product> findById(String id) {
        return Mono.justOrEmpty(store.get(id));
    }

    public Mono<Product> save(Product product) {
        return Mono.fromSupplier(() -> {
            Product toSave = product.id() == null ? product.withId(UUID.randomUUID().toString()) : product;
            store.put(toSave.id(), toSave);
            return toSave;
        });
    }

    public Mono<Boolean> deleteById(String id) {
        return Mono.fromSupplier(() -> store.remove(id) != null);
    }
}
