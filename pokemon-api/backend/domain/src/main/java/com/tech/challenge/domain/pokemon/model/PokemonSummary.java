package com.tech.challenge.domain.pokemon.model;

import java.math.BigDecimal;
import java.util.List;

public record PokemonSummary(
        int id,
        String name,
        String spriteUrl,
        String category,
        BigDecimal weightKg,
        List<String> abilities) {

    public PokemonSummary {
        if (id <= 0) {
            throw new IllegalArgumentException("Pokemon id must be positive");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Pokemon name must not be blank");
        }
        if (weightKg == null || weightKg.signum() < 0) {
            throw new IllegalArgumentException("Pokemon weight must not be null or negative");
        }
        abilities = abilities == null ? List.of() : List.copyOf(abilities);
    }
}
