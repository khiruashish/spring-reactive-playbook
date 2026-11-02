package com.example.fooddelivery.restaurant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.fooddelivery.restaurant.api.RestaurantHandler;
import com.example.fooddelivery.restaurant.api.RestaurantRouter;
import com.example.fooddelivery.restaurant.domain.MenuItem;
import com.example.fooddelivery.restaurant.domain.MenuItemRepository;
import com.example.fooddelivery.restaurant.domain.Restaurant;
import com.example.fooddelivery.restaurant.domain.RestaurantRepository;
import jakarta.validation.Validation;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Router + handler tested without starting a server: WebTestClient binds straight to the RouterFunction. */
class RestaurantRouterTest {

    private final RestaurantRepository restaurants = mock(RestaurantRepository.class);
    private final MenuItemRepository menuItems = mock(MenuItemRepository.class);
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        var handler = new RestaurantHandler(restaurants, menuItems,
                Validation.buildDefaultValidatorFactory().getValidator());
        client = WebTestClient.bindToRouterFunction(new RestaurantRouter().restaurantRoutes(handler)).build();
    }

    @Test
    void listsRestaurantsFilteredByCuisine() {
        when(restaurants.findByCuisineIgnoreCase("indian"))
                .thenReturn(Flux.just(new Restaurant(1L, "Spice Route", "Indian", "Bengaluru", true)));

        client.get().uri("/api/restaurants?cuisine=indian").accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].name").isEqualTo("Spice Route");
    }

    @Test
    void createsRestaurantAndReturnsLocation() {
        when(restaurants.save(any())).thenAnswer(inv -> {
            Restaurant r = inv.getArgument(0);
            return Mono.just(new Restaurant(42L, r.name(), r.cuisine(), r.address(), r.open()));
        });

        client.post().uri("/api/restaurants").accept(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Dosa Hub", "cuisine", "South Indian", "address", "Chennai"))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().location("/api/restaurants/42")
                .expectBody().jsonPath("$.id").isEqualTo(42);
    }

    @Test
    void invalidBodyBecomesProblemDetail() {
        client.post().uri("/api/restaurants").accept(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "", "cuisine", "Thai", "address", "Pune"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").isEqualTo("name must not be blank");
    }

    @Test
    void unknownRestaurantIs404() {
        when(restaurants.findById(99L)).thenReturn(Mono.empty());

        client.get().uri("/api/restaurants/99").accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void batchLooksUpMenuItems() {
        when(menuItems.findAllById(java.util.List.of(1L, 2L))).thenReturn(Flux.just(
                new MenuItem(1L, 1L, "Paneer", null, 24900, true),
                new MenuItem(2L, 1L, "Naan", null, 5900, true)));

        client.get().uri("/api/menu-items?ids=1&ids=2").accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(2);
    }
}
