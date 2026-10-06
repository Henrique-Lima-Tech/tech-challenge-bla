package com.tech.challenge.application.pokemon.port.in;

import com.tech.challenge.domain.pokemon.model.PokemonDetails;

public interface GetPokemonDetailsUseCase {

    PokemonDetails getDetails(String idOrName);
}
