package com.challenge.aitools.taskmanagement.infrastructure.user.security.token;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.challenge.aitools.taskmanagement.application.user.port.out.TokenIssuer;
import com.challenge.aitools.taskmanagement.domain.user.model.User;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtTokenIssuer implements TokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final Duration expiration;

    public JwtTokenIssuer(final JwtEncoder jwtEncoder,
            @Value("${security.jwt.expiration}") final Duration expiration) {
        if (expiration.isZero() || expiration.isNegative()) {
            throw new IllegalArgumentException("security.jwt.expiration must be positive");
        }
        log.info("Access tokens expire after {}", expiration);
        this.jwtEncoder = jwtEncoder;
        this.expiration = expiration;
    }

    @Override
    public String issue(final User user) {
        final var now = Instant.now();
        final var claims = JwtClaimsSet.builder()
                .subject(String.valueOf(user.id()))
                .issuedAt(now)
                .expiresAt(now.plus(expiration))
                .build();
        final var header = JwsHeader.with(MacAlgorithm.HS256).build();
        final var token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        log.debug("Issued an access token for user {}", user.id());
        return token;
    }
}
