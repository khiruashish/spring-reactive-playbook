# OAuth2 & JWT

## Resource server

```kotlin
implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
```

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://auth.example.com/realms/shop   # Keycloak, Cognito, Auth0, Okta…
          audiences: [catalog-api]
```

```java
@Bean
SecurityWebFilterChain api(ServerHttpSecurity http) {
    return http
            .authorizeExchange(a -> a
                    .pathMatchers(HttpMethod.GET, "/products/**").hasAuthority("SCOPE_catalog.read")
                    .anyExchange().authenticated())
            .oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()))
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .build();
}
```

Boot builds a `NimbusReactiveJwtDecoder` that fetches the issuer's JWKS (cached), validates signature,
`exp`/`nbf`, issuer and audience. Scopes become `SCOPE_*` authorities.

### Mapping roles from custom claims

```java
@Bean
ReactiveJwtAuthenticationConverterAdapter jwtConverter() {
    JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
    roles.setAuthoritiesClaimName("roles");
    roles.setAuthorityPrefix("ROLE_");
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(roles);
    return new ReactiveJwtAuthenticationConverterAdapter(converter);
}
// .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter())))
```

### Symmetric keys (local development)

```java
@Bean
ReactiveJwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret) {
    SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    return NimbusReactiveJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
}
```

### Tokens in query strings

Browsers can't send headers with `EventSource` or `WebSocket`. Allow `?access_token=` explicitly — and
only on those routes if possible (query strings end up in logs):

```java
var bearer = new ServerBearerTokenAuthenticationConverter();
bearer.setAllowUriQueryParameter(true);
http.oauth2ResourceServer(rs -> rs.bearerTokenConverter(bearer).jwt(Customizer.withDefaults()));
```

## OAuth2 client: calling other services with a token

```kotlin
implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
```

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          inventory:
            provider: keycloak
            client-id: catalog-service
            client-secret: ${CLIENT_SECRET}
            authorization-grant-type: client_credentials
        provider:
          keycloak:
            issuer-uri: https://auth.example.com/realms/shop
```

```java
@Bean
WebClient inventoryClient(WebClient.Builder builder, ReactiveOAuth2AuthorizedClientManager manager) {
    var oauth = new ServerOAuth2AuthorizedClientExchangeFilterFunction(manager);
    oauth.setDefaultClientRegistrationId("inventory");           // fetch + cache + refresh tokens
    return builder.clone().baseUrl("http://inventory:8080").filter(oauth).build();
}
```

To **relay the caller's token** instead (token propagation), read it from
`ReactiveSecurityContextHolder` in an `ExchangeFilterFunction` and set the `Authorization` header —
or let Spring Cloud Gateway's `TokenRelay` filter do it at the edge.
