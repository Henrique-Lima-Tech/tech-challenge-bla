package com.tech.challenge.domain.pokemon.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PokemonAlreadySyncedExceptionTest {

    @Test
    void shouldHaveFixedMessageWhenCreated() {
        // when
        final var exception = new PokemonAlreadySyncedException();

        // then
        assertThat(exception.getMessage()).isEqualTo("Pokemon already synced");
    }
}
