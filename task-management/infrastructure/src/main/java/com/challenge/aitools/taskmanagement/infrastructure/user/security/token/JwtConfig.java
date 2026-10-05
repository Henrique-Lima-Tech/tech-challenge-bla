package com.challenge.aitools.taskmanagement.infrastructure.user.security.token;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * The HS256 key shared by token issuing ({@link JwtTokenIssuer}) and token validation (the resource
 * server). The secret comes from {@code security.jwt.secret}, which has no default.
 */
@Configuration
public class JwtConfig {

    /** RFC 7518 section 3.2: an HS256 key must have at least 256 bits. */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey secretKey;

    public JwtConfig(@Value("${security.jwt.secret}") final String secret) {
        final var bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("security.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes");
        }
        this.secretKey = new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
