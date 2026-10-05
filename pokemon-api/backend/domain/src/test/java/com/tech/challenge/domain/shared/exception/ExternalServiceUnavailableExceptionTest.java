package com.tech.challenge.domain.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExternalServiceUnavailableExceptionTest {

    @Test
    void shouldKeepMessageAndCauseWhenCreated() {
        // given
        final var cause = new RuntimeException("connection refused");

        // when
        final var exception = new ExternalServiceUnavailableException("PokeAPI is unavailable", cause);

        // then
        assertThat(exception.getMessage()).isEqualTo("PokeAPI is unavailable");
        assertThat(exception.getCause()).isSameAs(cause);
    }
}
