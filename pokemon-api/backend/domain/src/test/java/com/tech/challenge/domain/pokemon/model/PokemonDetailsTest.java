package com.tech.challenge.domain.pokemon.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class PokemonDetailsTest {

    private static final EvolutionStage CHAIN = new EvolutionStage("bulbasaur", null, List.of());

    @Test
    void shouldCreateDetailsWhenDataIsValid() {
        // given
        final var stats = List.of(new PokemonStat("hp", 45));

        // when
        final var details = new PokemonDetails(1, "bulbasaur", "https://img/1.png", stats, "A seed.", CHAIN);

        // then
        assertThat(details.id()).isEqualTo(1);
        assertThat(details.name()).isEqualTo("bulbasaur");
        assertThat(details.imageUrl()).isEqualTo("https://img/1.png");
        assertThat(details.stats()).containsExactly(new PokemonStat("hp", 45));
        assertThat(details.description()).isEqualTo("A seed.");
        assertThat(details.evolutionChain()).isEqualTo(CHAIN);
    }

    @Test
    void shouldAcceptNullImageAndDescriptionWhenPokeApiHasNone() {
        // when
        final var details = new PokemonDetails(1, "bulbasaur", null, List.of(), null, CHAIN);

        // then
        assertThat(details.imageUrl()).isNull();
        assertThat(details.description()).isNull();
    }

    @Test
    void shouldRejectDetailsWhenIdIsNotPositive() {
        // when & then
        assertThatThrownBy(() -> new PokemonDetails(0, "bulbasaur", null, List.of(), null, CHAIN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectDetailsWhenNameIsBlank() {
        // when & then
        assertThatThrownBy(() -> new PokemonDetails(1, " ", null, List.of(), null, CHAIN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PokemonDetails(1, null, null, List.of(), null, CHAIN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectDetailsWhenEvolutionChainIsNull() {
        // when & then
        assertThatThrownBy(() -> new PokemonDetails(1, "bulbasaur", null, List.of(), null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldUseEmptyStatsWhenStatsAreNull() {
        // when
        final var details = new PokemonDetails(1, "bulbasaur", null, null, null, CHAIN);

        // then
        assertThat(details.stats()).isEmpty();
    }

    @Test
    void shouldKeepAnImmutableCopyOfStatsWhenSourceListChanges() {
        // given
        final var stats = new ArrayList<>(List.of(new PokemonStat("hp", 45)));

        // when
        final var details = new PokemonDetails(1, "bulbasaur", null, stats, null, CHAIN);
        stats.add(new PokemonStat("attack", 49));

        // then
        assertThat(details.stats()).hasSize(1);
        assertThatThrownBy(() -> details.stats().add(new PokemonStat("speed", 45)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
