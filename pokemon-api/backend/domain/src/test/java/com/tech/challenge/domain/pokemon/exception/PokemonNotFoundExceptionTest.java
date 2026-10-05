package com.tech.challenge.domain.pokemon.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PokemonNotFoundExceptionTest {

    @Test
    void shouldExposeIdOrNameWhenCreated() {
        // when
        final var exception = new PokemonNotFoundException("missingno");

        // then
        assertThat(exception.getIdOrName()).isEqualTo("missingno");
        assertThat(exception.getMessage()).isEqualTo("Pokemon not found: missingno");
    }
}
