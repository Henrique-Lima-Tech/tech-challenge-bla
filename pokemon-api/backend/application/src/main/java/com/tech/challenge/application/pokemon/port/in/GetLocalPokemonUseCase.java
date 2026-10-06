package com.tech.challenge.application.pokemon.port.in;

import com.tech.challenge.application.pokemon.result.LocalPokemonResult;

public interface GetLocalPokemonUseCase {

    LocalPokemonResult get(long userId, long id);
}
