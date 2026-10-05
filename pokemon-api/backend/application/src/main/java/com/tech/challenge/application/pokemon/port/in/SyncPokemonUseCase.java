package com.tech.challenge.application.pokemon.port.in;

import com.tech.challenge.application.pokemon.command.SyncPokemonCommand;
import com.tech.challenge.application.pokemon.result.LocalPokemonResult;

/**
 * REQ-US03: copies a Pokémon from the PokéAPI into the local database, with the proprietary fields.
 */
public interface SyncPokemonUseCase {

    /**
     * @throws com.tech.challenge.domain.pokemon.exception.PokemonNotFoundException if the PokéAPI does not know it
     * @throws com.tech.challenge.domain.pokemon.exception.PokemonAlreadySyncedException if the user already has it
     * @throws com.tech.challenge.domain.shared.exception.ExternalServiceUnavailableException if the PokéAPI cannot answer
     */
    LocalPokemonResult sync(SyncPokemonCommand command);
}
