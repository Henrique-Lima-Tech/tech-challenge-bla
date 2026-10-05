package com.tech.challenge.infrastructure.user.security.token;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.tech.challenge.application.user.port.out.TokenPort;
import com.tech.challenge.application.user.result.AccessTokenResult;
import com.tech.challenge.domain.user.model.User;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtTokenAdapter implements TokenPort {

    private final JwtEncoder jwtEncoder;
    private final Duration expiration;

    public JwtTokenAdapter(final JwtEncoder jwtEncoder, @Value("${security.jwt.expiration}") final Duration expiration) {
        if (expiration.isZero() || expiration.isNegative()) {
            throw new IllegalArgumentException("security.jwt.expiration must be positive");
        }
        log.info("JWT expiration: {}", expiration);
        this.jwtEncoder = jwtEncoder;
        this.expiration = expiration;
    }

    @Override
    public AccessTokenResult issue(final User user) {
        final var now = Instant.now();
        final var claims = JwtClaimsSet.builder()
                .subject(String.valueOf(user.id()))
                .issuedAt(now)
                .expiresAt(now.plus(expiration))
                .build();
        final var header = JwsHeader.with(MacAlgorithm.HS256).build();
        final var token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        log.debug("Issued access token for user {}", user.id());
        return new AccessTokenResult(token, expiration.toSeconds());
    }
}
