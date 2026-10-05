package com.tech.challenge.domain.pokemon.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class EvolutionStageTest {

    @Test
    void shouldKeepBranchesWhenStageEvolvesIntoSeveralPokemon() {
        // when
        final var stage = new EvolutionStage("eevee", null, List.of(
                new EvolutionStage("vaporeon", null, List.of()),
                new EvolutionStage("jolteon", null, List.of())));

        // then
        assertThat(stage.evolvesTo()).extracting(EvolutionStage::name).containsExactly("vaporeon", "jolteon");
    }

    @Test
    void shouldHaveEmptyEvolvesToWhenStageIsALeaf() {
        // when
        final var stage = new EvolutionStage("venusaur", null, null);

        // then
        assertThat(stage.evolvesTo()).isEmpty();
    }

    @Test
    void shouldRejectStageWhenNameIsBlank() {
        // when & then
        assertThatThrownBy(() -> new EvolutionStage(" ", null, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EvolutionStage(null, null, List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldKeepAnImmutableCopyOfEvolvesToWhenSourceListChanges() {
        // given
        final var children = new ArrayList<>(List.of(new EvolutionStage("ivysaur", null, List.of())));

        // when
        final var stage = new EvolutionStage("bulbasaur", null, children);
        children.clear();

        // then
        assertThat(stage.evolvesTo()).hasSize(1);
        assertThatThrownBy(() -> stage.evolvesTo().add(new EvolutionStage("x", null, List.of())))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldKeepSpriteUrlWhenStageHasASprite() {
        // when
        final var stage = new EvolutionStage("bulbasaur",
                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png", List.of());

        // then
        assertThat(stage.spriteUrl())
                .isEqualTo("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png");
    }

    @Test
    void shouldAllowNullSpriteUrlWhenSpriteIsUnknown() {
        // when
        final var stage = new EvolutionStage("bulbasaur", null, List.of());

        // then
        assertThat(stage.spriteUrl()).isNull();
        assertThat(stage.name()).isEqualTo("bulbasaur");
    }
}
