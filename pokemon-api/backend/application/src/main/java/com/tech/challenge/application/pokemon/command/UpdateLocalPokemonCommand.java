package com.tech.challenge.application.pokemon.command;

import java.math.BigDecimal;
import java.util.List;

public record UpdateLocalPokemonCommand(
        long userId,
        long id,
        String name,
        String spriteUrl,
        String category,
        BigDecimal weightKg,
        List<String> abilities,
        String localizedName,
        String region,
        List<String> internalTags) {
}
