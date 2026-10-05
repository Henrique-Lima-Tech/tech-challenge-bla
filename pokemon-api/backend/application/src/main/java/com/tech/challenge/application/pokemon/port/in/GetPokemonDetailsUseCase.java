package com.tech.challenge.application.pokemon.port.in;

import com.tech.challenge.domain.pokemon.model.PokemonDetails;

/**
 * REQ-US02: comprehensive data for a chosen Pokémon.
 */
public interface GetPokemonDetailsUseCase {

    /**
     * @throws com.tech.challenge.domain.pokemon.exception.PokemonNotFoundException if the PokéAPI does not know it
     * @throws com.tech.challenge.domain.shared.exception.ExternalServiceUnavailableException if the PokéAPI cannot answer
     */
    PokemonDetails getDetails(String idOrName);
}
