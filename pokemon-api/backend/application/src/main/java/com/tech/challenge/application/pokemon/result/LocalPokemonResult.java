package com.tech.challenge.application.pokemon.result;

import java.math.BigDecimal;
import java.util.List;

import com.tech.challenge.domain.pokemon.model.LocalPokemon;

public record LocalPokemonResult(
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

    public static LocalPokemonResult from(final LocalPokemon pokemon) {
        return new LocalPokemonResult(pokemon.id(), pokemon.pokeApiId(), pokemon.name(), pokemon.spriteUrl(),
                pokemon.category(), pokemon.weightKg(), pokemon.abilities(), pokemon.localizedName(), pokemon.region(),
                pokemon.internalTags());
    }
}
