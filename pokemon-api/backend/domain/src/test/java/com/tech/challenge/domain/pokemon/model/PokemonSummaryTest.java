package com.tech.challenge.domain.pokemon.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class PokemonSummaryTest {

    private static final BigDecimal WEIGHT = new BigDecimal("6.9");

    @Test
    void shouldCreateSummaryWhenDataIsValid() {
        // when
        final var summary = new PokemonSummary(1, "bulbasaur", "https://img/1.png", "Seed Pokémon", WEIGHT,
                List.of("overgrow", "chlorophyll"));

        // then
        assertThat(summary.id()).isEqualTo(1);
        assertThat(summary.name()).isEqualTo("bulbasaur");
        assertThat(summary.spriteUrl()).isEqualTo("https://img/1.png");
        assertThat(summary.category()).isEqualTo("Seed Pokémon");
        assertThat(summary.weightKg()).isEqualTo(WEIGHT);
        assertThat(summary.abilities()).containsExactly("overgrow", "chlorophyll");
    }

    @Test
    void shouldAcceptNullSpriteAndCategoryWhenPokeApiHasNone() {
        // when
        final var summary = new PokemonSummary(1, "bulbasaur", null, null, WEIGHT, List.of());

        // then
        assertThat(summary.spriteUrl()).isNull();
        assertThat(summary.category()).isNull();
    }

    @Test
    void shouldRejectSummaryWhenIdIsNotPositive() {
        // when & then
        assertThatThrownBy(() -> new PokemonSummary(0, "bulbasaur", null, null, WEIGHT, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectSummaryWhenNameIsBlank() {
        // when & then
        assertThatThrownBy(() -> new PokemonSummary(1, " ", null, null, WEIGHT, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PokemonSummary(1, null, null, null, WEIGHT, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectSummaryWhenWeightIsNullOrNegative() {
        // when & then
        assertThatThrownBy(() -> new PokemonSummary(1, "bulbasaur", null, null, null, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PokemonSummary(1, "bulbasaur", null, null, new BigDecimal("-0.1"), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldUseEmptyAbilitiesWhenAbilitiesAreNull() {
        // when
        final var summary = new PokemonSummary(1, "bulbasaur", null, null, WEIGHT, null);

        // then
        assertThat(summary.abilities()).isEmpty();
    }

    @Test
    void shouldKeepAnImmutableCopyOfAbilitiesWhenSourceListChanges() {
        // given
        final var abilities = new ArrayList<>(List.of("overgrow"));

        // when
        final var summary = new PokemonSummary(1, "bulbasaur", null, null, WEIGHT, abilities);
        abilities.add("chlorophyll");

        // then
        assertThat(summary.abilities()).containsExactly("overgrow");
        assertThatThrownBy(() -> summary.abilities().add("x")).isInstanceOf(UnsupportedOperationException.class);
    }
}
