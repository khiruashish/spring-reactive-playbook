package com.example.fooddelivery.gateway;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    // --8<-- [start:chain]
    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        // Browsers cannot set headers on EventSource/WebSocket, so also accept ?access_token=…
        var bearer = new ServerBearerTokenAuthenticationConverter();
        bearer.setAllowUriQueryParameter(true);

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)              // stateless bearer-token API
                .authorizeExchange(auth -> auth
                        .pathMatchers("/", "/index.html", "/favicon.ico", "/auth/token", "/actuator/health/**").permitAll()
                        .pathMatchers(org.springframework.http.HttpMethod.GET, "/api/restaurants/**").permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(rs -> rs
                        .bearerTokenConverter(bearer)
                        .jwt(jwt -> { }))
                .build();
    }
    // --8<-- [end:chain]

    @Bean
    SecretKey jwtKey(JwtProperties props) {
        return new SecretKeySpec(props.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder(SecretKey jwtKey) {
        return NimbusReactiveJwtDecoder.withSecretKey(jwtKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return NimbusJwtEncoder.withSecretKey(jwtKey).algorithm(MacAlgorithm.HS256).build();
    }
}
