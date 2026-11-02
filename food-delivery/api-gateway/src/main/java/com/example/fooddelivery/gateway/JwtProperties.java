package com.example.fooddelivery.gateway;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Local-dev token settings. The gateway signs HS256 tokens itself so the demo runs with zero external
 * dependencies; in production point {@code spring.security.oauth2.resourceserver.jwt.issuer-uri} at
 * Keycloak/Cognito/Auth0 and delete {@link TokenController}.
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, Duration ttl) {
}
