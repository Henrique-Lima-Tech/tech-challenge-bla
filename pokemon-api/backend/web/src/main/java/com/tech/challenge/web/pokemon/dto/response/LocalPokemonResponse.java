package com.tech.challenge.web.pokemon.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record LocalPokemonResponse(
        long id,
        int pokeApiId,
        String name,
        String spriteUrl,
        String category,
        BigDecimal weightKg,
        List<String> abilities,
        String localizedName,
        String region,
        List<String> internalTags) {
}
