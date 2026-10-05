package com.tech.challenge.application.pokemon.command;

import java.math.BigDecimal;
import java.util.List;

/**
 * Every field a {@code PUT} replaces, plus the user and the id of the record to replace. The identifiers
 * {@code id} and {@code pokeApiId} of the stored record are never changed (D-25).
 */
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
