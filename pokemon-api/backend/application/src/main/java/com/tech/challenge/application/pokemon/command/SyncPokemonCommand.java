package com.tech.challenge.application.pokemon.command;

import java.util.List;

/**
 * @param userId  the user the copy will belong to (D-31)
 * @param pokemon PokéAPI id or name of the Pokémon to copy
 */
public record SyncPokemonCommand(long userId, String pokemon, String localizedName, String region,
        List<String> internalTags) {
}
