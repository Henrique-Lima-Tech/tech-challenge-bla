package com.tech.challenge.domain.pokemon.model;

import java.util.List;

public record EvolutionStage(String name, String spriteUrl, List<EvolutionStage> evolvesTo) {

    public EvolutionStage {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Evolution stage name must not be blank");
        }
        evolvesTo = evolvesTo == null ? List.of() : List.copyOf(evolvesTo);
    }
}
