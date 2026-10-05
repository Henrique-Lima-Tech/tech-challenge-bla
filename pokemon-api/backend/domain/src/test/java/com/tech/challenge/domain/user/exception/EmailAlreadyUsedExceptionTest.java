package com.tech.challenge.domain.user.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailAlreadyUsedExceptionTest {

    @Test
    void shouldHaveFixedMessageWhenCreated() {
        // when
        final var exception = new EmailAlreadyUsedException();

        // then
        assertThat(exception.getMessage()).isEqualTo("Email already used");
    }
}
