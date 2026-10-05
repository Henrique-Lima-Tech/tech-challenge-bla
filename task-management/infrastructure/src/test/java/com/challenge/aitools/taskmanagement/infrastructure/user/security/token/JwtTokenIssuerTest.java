package com.challenge.aitools.taskmanagement.infrastructure.user.security.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.challenge.aitools.taskmanagement.domain.user.model.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

class JwtTokenIssuerTest {

    private static final String SECRET = "test-only-secret-with-at-least-32-bytes!";
    private static final User USER = new User(7L, "Demo User", "demo@example.com", "$2a$10$hash");

    private final SecretKeySpec key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    private final JwtTokenIssuer issuer = new JwtTokenIssuer(new NimbusJwtEncoder(new ImmutableSecret<>(key)),
            Duration.ofHours(1));

    @Test
    void shouldIssueATokenWithTheUserIdAsSubjectWhenIssuing() {
        // when
        final var token = issuer.issue(USER);

        // then
        assertThat(decoder().decode(token).getSubject()).isEqualTo("7");
    }

    @Test
    void shouldIssueATokenValidForOneHourWhenIssuing() {
        // when
        final var token = issuer.issue(USER);

        // then
        final var jwt = decoder().decode(token);
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void shouldSignTheTokenWithHs256WhenIssuing() {
        // when
        final var token = issuer.issue(USER);

        // then
        assertThat(decoder().decode(token).getHeaders()).containsEntry("alg", MacAlgorithm.HS256.getName());
    }

    @Test
    void shouldRejectTheIssuerWhenExpirationIsNotPositive() {
        // when & then
        assertThatThrownBy(() -> new JwtTokenIssuer(new NimbusJwtEncoder(new ImmutableSecret<>(key)), Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private JwtDecoder decoder() {
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
