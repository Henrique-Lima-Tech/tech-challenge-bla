package com.tech.challenge.web.pokemon.dto.response;

import java.util.List;

public record EvolutionStageResponse(String name, String spriteUrl, List<EvolutionStageResponse> evolvesTo) {
}
