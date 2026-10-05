package com.tech.challenge.application.pokemon.result;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tech.challenge.domain.pokemon.model.LocalPokemon;

class LocalPokemonResultTest {

    @Test
    void shouldCopyEveryFieldWhenBuiltFromLocalPokemon() {
        // given
        final var pokemon = new LocalPokemon(10L, 25, "pikachu", "https://img/25.png", "Mouse Pokémon",
                new BigDecimal("6.0"), List.of("static"), "ピカチュウ", "Kanto", List.of("starter"));

        // when
        final var result = LocalPokemonResult.from(pokemon);

        // then
        assertThat(result).isEqualTo(new LocalPokemonResult(10L, 25, "pikachu", "https://img/25.png", "Mouse Pokémon",
                new BigDecimal("6.0"), List.of("static"), "ピカチュウ", "Kanto", List.of("starter")));
    }
}
