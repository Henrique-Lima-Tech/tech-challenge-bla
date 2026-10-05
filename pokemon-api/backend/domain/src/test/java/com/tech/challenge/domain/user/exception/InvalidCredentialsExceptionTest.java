package com.tech.challenge.domain.user.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InvalidCredentialsExceptionTest {

    @Test
    void shouldHaveFixedMessageWhenCreated() {
        // when
        final var exception = new InvalidCredentialsException();

        // then
        assertThat(exception.getMessage()).isEqualTo("Invalid credentials");
    }
}
