rootProject.name = "spring-reactive-webapp"

dependencyResolutionManagement {
    repositories { mavenCentral() }
}

// Runnable, test-backed code for every Reactor/WebFlux example embedded in the playbook.
include("playbook-examples")

// The capstone: a reactive, event-driven food delivery platform.
include(
    "food-delivery:common-events",
    "food-delivery:api-gateway",
    "food-delivery:restaurant-service",
    "food-delivery:order-service",
    "food-delivery:payment-service",
    "food-delivery:delivery-service",
    "food-delivery:notification-service",
)
