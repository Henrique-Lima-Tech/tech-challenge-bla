package com.tech.challenge.application.pokemon.command;

import java.util.List;

public record SyncPokemonCommand(long userId, String pokemon, String localizedName, String region,
        List<String> internalTags) {
}
