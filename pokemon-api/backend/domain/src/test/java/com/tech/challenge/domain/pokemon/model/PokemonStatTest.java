package com.tech.challenge.domain.pokemon.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PokemonStatTest {

    @Test
    void shouldCreateStatWhenDataIsValid() {
        // when
        final var stat = new PokemonStat("hp", 45);

        // then
        assertThat(stat.name()).isEqualTo("hp");
        assertThat(stat.baseStat()).isEqualTo(45);
    }

    @Test
    void shouldRejectStatWhenNameIsBlank() {
        // when & then
        assertThatThrownBy(() -> new PokemonStat("", 45)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PokemonStat(null, 45)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectStatWhenBaseStatIsNegative() {
        // when & then
        assertThatThrownBy(() -> new PokemonStat("hp", -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
