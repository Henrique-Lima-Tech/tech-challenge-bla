package com.challenge.aitools.taskmanagement.infrastructure.user.security.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtConfigTest {

    @Test
    void shouldBuildAnEncoderAndADecoderWhenSecretIsLongEnough() {
        // given
        final var config = new JwtConfig("test-only-secret-with-at-least-32-bytes!");

        // when & then
        assertThat(config.jwtEncoder()).isNotNull();
        assertThat(config.jwtDecoder()).isNotNull();
    }

    @Test
    void shouldRejectTheConfigurationWhenSecretIsTooShort() {
        // when & then
        assertThatThrownBy(() -> new JwtConfig("too-short"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
