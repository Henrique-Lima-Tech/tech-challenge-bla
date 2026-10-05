package com.tech.challenge.domain.pokemon.model;

import java.util.List;

/**
 * Comprehensive data of one Pokémon (REQ-US02): image, core statistics,
 * narrative description and evolutionary lineage.
 */
public record PokemonDetails(
        int id,
        String name,
        String imageUrl,
        List<PokemonStat> stats,
        String description,
        EvolutionStage evolutionChain) {

    public PokemonDetails {
        if (id <= 0) {
            throw new IllegalArgumentException("Pokemon id must be positive");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Pokemon name must not be blank");
        }
        if (evolutionChain == null) {
            throw new IllegalArgumentException("Pokemon evolution chain must not be null");
        }
        stats = stats == null ? List.of() : List.copyOf(stats);
    }
}
