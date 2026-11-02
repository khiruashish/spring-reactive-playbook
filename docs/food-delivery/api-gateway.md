# API Gateway

**Role:** single entry point — authenticates users, relays identity, routes HTTP/SSE/WebSocket traffic,
serves the UI. **Topics:** [Spring Cloud Gateway](../messaging/gateway.md),
[WebFlux Security](../security/webflux-security.md), [OAuth2 & JWT](../security/oauth2-jwt.md),
[Context](../reactor/context.md).

## Security chain

```java
--8<-- "food-delivery/api-gateway/src/main/java/com/example/fooddelivery/gateway/SecurityConfig.java:chain"
```

Restaurant browsing is public; everything else needs a JWT. Because `EventSource` and `WebSocket` cannot
send headers, the token may also arrive as `?access_token=`.

!!! warning "Dev-only token endpoint"
    `POST /auth/token` signs an HS256 token for any username so the demo runs with no identity provider.
    In production, delete `TokenController` and point `spring.security.oauth2.resourceserver.jwt.issuer-uri`
    at Keycloak/Cognito/Auth0.

## Identity relay — reading the security context reactively

```java
--8<-- "food-delivery/api-gateway/src/main/java/com/example/fooddelivery/gateway/UserIdRelayFilter.java:filter"
```

Downstream services trust `X-User-Id` **only because** the gateway strips any client-supplied value
first and the services are not exposed publicly.

## Routes

```yaml
--8<-- "food-delivery/api-gateway/src/main/resources/application.yml"
```

Note the `ws://` route for notifications, and `Retry` only on idempotent `GET`s.

## The UI

`src/main/resources/static/index.html` — ~150 lines of vanilla JS: fetches restaurants and menus,
places orders with a fresh `Idempotency-Key`, then opens an `EventSource` on the order's status stream
and a `WebSocket` for notifications.
