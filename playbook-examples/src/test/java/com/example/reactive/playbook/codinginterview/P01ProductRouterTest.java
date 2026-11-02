package com.example.reactive.playbook.codinginterview;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class P01ProductRouterTest {

    WebTestClient client;

    @BeforeEach
    void setUp() {
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        client = WebTestClient.bindToRouterFunction(new P01ProductRouter(validator).routes()).build();
    }

    @Test
    void create_returns201_withLocation() {
        client.post().uri("/products").bodyValue(new P01ProductRouter.CreateProduct("Kettle", 2_500)).exchange()
              .expectStatus().isCreated()
              .expectHeader().valueMatches("Location", "/products/\\d+")
              .expectBody().jsonPath("$.name").isEqualTo("Kettle");
    }

    @Test
    void invalidBody_returns400_withEveryViolation() {
        client.post().uri("/products").bodyValue(new P01ProductRouter.CreateProduct("", -1)).exchange()
              .expectStatus().isBadRequest()
              .expectBody().jsonPath("$.detail").isEqualTo("name must not be blank; priceCents must be greater than 0");
    }

    @Test
    void missingProduct_returns404_problemDetail() {
        client.get().uri("/products/42").exchange()
              .expectStatus().isNotFound()
              .expectBody().jsonPath("$.detail").isEqualTo("product 42 not found");
    }

    @Test
    void roundTrip_createGetDelete() {
        String location = client.post().uri("/products").bodyValue(new P01ProductRouter.CreateProduct("Mug", 900)).exchange()
                .expectStatus().isCreated().returnResult(Void.class).getResponseHeaders().getLocation().toString();
        client.get().uri(location).exchange().expectStatus().isOk().expectBody().jsonPath("$.priceCents").isEqualTo(900);
        client.delete().uri(location).exchange().expectStatus().isNoContent();
        client.get().uri(location).exchange().expectStatus().isNotFound();
    }
}
