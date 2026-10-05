package com.tech.challenge.domain.pokemon.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LocalPokemonNotFoundExceptionTest {

    @Test
    void shouldHaveFixedMessageWithTheIdWhenCreated() {
        // when
        final var exception = new LocalPokemonNotFoundException(10L);

        // then
        assertThat(exception.getMessage()).isEqualTo("Local Pokemon not found: 10");
        assertThat(exception.getId()).isEqualTo(10L);
    }
}
