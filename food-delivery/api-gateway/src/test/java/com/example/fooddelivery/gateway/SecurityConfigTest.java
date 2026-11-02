package com.example.fooddelivery.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

/** Token issued by the dev endpoint must round-trip through the gateway's JWT decoder. */
class SecurityConfigTest {

    private final SecurityConfig config = new SecurityConfig();
    private final JwtProperties props = new JwtProperties("test-secret-test-secret-test-secret-123", Duration.ofMinutes(5));

    @Test
    void issuedTokenIsAcceptedByDecoder() {
        var key = config.jwtKey(props);
        var controller = new TokenController(config.jwtEncoder(key), props);

        StepVerifier.create(controller.token(new TokenController.TokenRequest("alice"))
                        .flatMap(t -> config.jwtDecoder(key).decode(t.accessToken())))
                .assertNext(jwt -> {
                    assertThat(jwt.getSubject()).isEqualTo("alice");
                    assertThat(jwt.getClaimAsString("iss")).isEqualTo("food-delivery-gateway");
                })
                .verifyComplete();
    }

    @Test
    void rejectsSuspiciousUsernames() {
        var controller = new TokenController(config.jwtEncoder(config.jwtKey(props)), props);
        StepVerifier.create(controller.token(new TokenController.TokenRequest("<script>")))
                .expectError()
                .verify();
    }
}
