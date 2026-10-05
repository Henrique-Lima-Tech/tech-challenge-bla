package com.tech.challenge.infrastructure.user.security.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtException;

import com.tech.challenge.domain.user.model.User;

class JwtTokenAdapterTest {

    private static final String SECRET = "test-only-secret-with-at-least-32-bytes!";
    private static final User ASH = new User(7L, "Ash", "ash@example.com", "$2a$10$hash");

    private final JwtConfig jwtConfig = new JwtConfig(SECRET);
    private final JwtTokenAdapter adapter = new JwtTokenAdapter(jwtConfig.jwtEncoder(), Duration.ofHours(1));

    @Test
    void shouldIssueTokenAcceptedByDecoderWhenUserLogsIn() {
        // when
        final var token = adapter.issue(ASH);

        // then
        final var jwt = jwtConfig.jwtDecoder().decode(token.value());
        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getHeaders()).containsEntry("alg", MacAlgorithm.HS256.getName());
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(1));
        assertThat(token.expiresInSeconds()).isEqualTo(3600);
    }

    @Test
    void shouldNotPutEmailOrNameInTokenWhenIssued() {
        // when
        final var token = adapter.issue(ASH);

        // then
        final var jwt = jwtConfig.jwtDecoder().decode(token.value());
        assertThat(jwt.getClaims()).containsOnlyKeys("sub", "iat", "exp");
    }

    @Test
    void shouldRejectTokenWhenSignedWithAnotherKey() {
        // given
        final var otherConfig = new JwtConfig("another-test-secret-with-at-least-32-bytes");
        final var token = new JwtTokenAdapter(otherConfig.jwtEncoder(), Duration.ofHours(1)).issue(ASH);

        // when & then
        assertThatThrownBy(() -> jwtConfig.jwtDecoder().decode(token.value())).isInstanceOf(JwtException.class);
    }

    @ParameterizedTest
    @ValueSource(longs = { 0, -1 })
    void shouldRejectExpirationWhenNotPositive(final long seconds) {
        // when & then
        assertThatThrownBy(() -> new JwtTokenAdapter(jwtConfig.jwtEncoder(), Duration.ofSeconds(seconds)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
