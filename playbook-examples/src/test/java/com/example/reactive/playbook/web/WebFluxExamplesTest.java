package com.example.reactive.playbook.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.reactive.playbook.web.InventoryClient.InventoryUnavailableException;
import com.example.reactive.playbook.web.InventoryClient.Stock;
import com.example.reactive.playbook.web.PriceStreamController.PriceTick;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class WebFluxExamplesTest {

    private final ProductRepository repository = new ProductRepository();
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        // --8<-- [start:bindToController]
        client = WebTestClient
                .bindToController(new ProductController(repository), new PriceStreamController())
                .controllerAdvice(new GlobalErrorHandler())
                .webFilter(new RequestIdWebFilter())
                .validator(validator)
                .build();
        // --8<-- [end:bindToController]
    }

    @Test
    void createThenFetch() {
        // --8<-- [start:webTestClient]
        Product created = client.post().uri("/products")
                .bodyValue(new Product(null, "Keyboard", "electronics", 4_999))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(RequestIdWebFilter.HEADER)
                .expectBody(Product.class)
                .returnResult().getResponseBody();

        client.get().uri("/products/{id}", created.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Keyboard")
                .jsonPath("$.priceMinor").isEqualTo(4999);
        // --8<-- [end:webTestClient]
    }

    @Test
    void missingProductIsProblemDetail() {
        client.get().uri("/products/nope").exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.title").isEqualTo("Product not found");
    }

    @Test
    void validationErrorsAreListed() {
        client.post().uri("/products")
                .bodyValue(new Product(null, "", "electronics", -1))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors[0]").isEqualTo("name: must not be blank")
                .jsonPath("$.errors[1]").isEqualTo("priceMinor: must be greater than or equal to 0");
    }

    @Test
    void serverSentEventsCanBeConsumedAsFlux() {
        // --8<-- [start:sseTest]
        Flux<ServerSentEvent<PriceTick>> events = client.get().uri("/products/p1/prices")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<ServerSentEvent<PriceTick>>() { })
                .getResponseBody();

        StepVerifier.create(events.take(2))      // take(2) cancels the infinite stream
                .assertNext(e -> assertThat(e.event()).isEqualTo("price"))
                .assertNext(e -> assertThat(e.data().sequence()).isEqualTo(1))
                .verifyComplete();
        // --8<-- [end:sseTest]
    }

    @Test
    void functionalRoutesWithFilter() {
        // --8<-- [start:routerTest]
        WebTestClient routes = WebTestClient.bindToRouterFunction(new ProductRouter().productRoutes(repository)).build();

        routes.post().uri("/v2/products").bodyValue(new Product(null, "Mouse", "electronics", 1_999))
                .exchange().expectStatus().isUnauthorized();               // filter rejects writes without key

        routes.post().uri("/v2/products").header("X-Api-Key", "secret")
                .bodyValue(new Product(null, "Mouse", "electronics", 1_999))
                .exchange().expectStatus().isCreated();

        routes.get().uri("/v2/products").accept(MediaType.APPLICATION_JSON)
                .exchange().expectStatus().isOk()
                .expectBodyList(Product.class).hasSize(1);
        // --8<-- [end:routerTest]
    }

    @Test
    void webClientRetriesTransientErrorsThenSucceeds() {
        // --8<-- [start:webClientTest]
        // Stub the transport with an ExchangeFunction: no server, no mock-server library needed
        AtomicInteger calls = new AtomicInteger();
        ExchangeFunction flakyThenOk = request -> Mono.just(calls.incrementAndGet() < 3
                ? ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE).build()
                : ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"productId\":\"p1\",\"available\":7}")
                        .build());
        var inventory = new InventoryClient(WebClient.builder().exchangeFunction(flakyThenOk).build());

        StepVerifier.create(inventory.stock("p1"))
                .expectNext(new Stock("p1", 7))
                .verifyComplete();
        assertThat(calls).hasValue(3);   // two 503s retried, third call succeeded
        // --8<-- [end:webClientTest]
    }

    @Test
    void webClientMapsExhaustedRetriesAndNotFound() {
        var down = new InventoryClient(WebClient.builder()
                .exchangeFunction(r -> Mono.just(ClientResponse.create(HttpStatus.BAD_GATEWAY).build())).build());
        StepVerifier.create(down.stock("p1")).verifyError(InventoryUnavailableException.class);

        var missing = new InventoryClient(WebClient.builder()
                .exchangeFunction(r -> Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build())).build());
        StepVerifier.create(missing.stock("p1")).verifyComplete();
    }
}
