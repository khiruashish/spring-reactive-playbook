package com.example.fooddelivery.gateway;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** DEV ONLY: issues a signed token for any username so the demo needs no identity provider. */
@RestController
class TokenController {

    record TokenRequest(String username) {
    }

    record TokenResponse(String accessToken, String tokenType, long expiresIn) {
    }

    private final JwtEncoder encoder;
    private final JwtProperties props;

    TokenController(JwtEncoder encoder, JwtProperties props) {
        this.encoder = encoder;
        this.props = props;
    }

    @PostMapping("/auth/token")
    Mono<TokenResponse> token(@RequestBody TokenRequest request) {
        if (request.username() == null || !request.username().matches("[a-zA-Z0-9._-]{2,40}")) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "username: 2-40 chars [a-zA-Z0-9._-]"));
        }
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("food-delivery-gateway")
                .subject(request.username())
                .issuedAt(now)
                .expiresAt(now.plus(props.ttl()))
                .claim("scope", "orders")
                .build();
        var params = JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims);
        // signing is CPU work, not I/O — cheap, but keep it off the event loop by habit
        return Mono.fromCallable(() -> encoder.encode(params).getTokenValue())
                .subscribeOn(Schedulers.parallel())
                .map(token -> new TokenResponse(token, "Bearer", props.ttl().toSeconds()));
    }
}
