package com.tech.challenge.domain.pokemon.model;

import java.util.List;

/**
 * One node of an evolution chain. Branches are kept: a stage may evolve into several Pokémon.
 * {@code spriteUrl} is {@code null} when the sprite is unknown (D-29).
 */
public record EvolutionStage(String name, String spriteUrl, List<EvolutionStage> evolvesTo) {

    public EvolutionStage {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Evolution stage name must not be blank");
        }
        evolvesTo = evolvesTo == null ? List.of() : List.copyOf(evolvesTo);
    }
}
