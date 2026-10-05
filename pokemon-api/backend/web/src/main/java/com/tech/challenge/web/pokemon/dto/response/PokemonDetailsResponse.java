package com.tech.challenge.web.pokemon.dto.response;

import java.util.List;

public record PokemonDetailsResponse(
        int id,
        String name,
        String imageUrl,
        List<StatResponse> stats,
        String description,
        EvolutionStageResponse evolutionChain) {
}
