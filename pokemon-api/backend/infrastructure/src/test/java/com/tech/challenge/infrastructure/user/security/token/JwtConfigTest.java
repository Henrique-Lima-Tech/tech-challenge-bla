package com.tech.challenge.infrastructure.user.security.token;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

class JwtConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(JwtConfig.class);

    @Test
    void shouldCreateEncoderAndDecoderWhenSecretIsLongEnough() {
        // when & then
        contextRunner.withPropertyValues("security.jwt.secret=test-only-secret-with-at-least-32-bytes!")
                .run(context -> assertThat(context).hasSingleBean(JwtEncoder.class).hasSingleBean(JwtDecoder.class));
    }

    @Test
    void shouldFailAtStartupWhenSecretIsMissing() {
        // when & then
        contextRunner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shouldFailAtStartupWithoutPrintingSecretWhenSecretIsShorterThan32Bytes() {
        // when & then
        contextRunner.withPropertyValues("security.jwt.secret=short-secret-value")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).rootCause()
                            .hasMessage("security.jwt.secret must be at least 32 bytes")
                            .hasMessageNotContaining("short-secret-value");
                });
    }
}
