package com.tech.challenge.web.pokemon.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record PokemonSummaryResponse(
        int id,
        String name,
        String spriteUrl,
        String category,
        BigDecimal weightKg,
        List<String> abilities) {
}
