package com.tech.challenge.application.pokemon.port.in;

import com.tech.challenge.application.pokemon.command.UpdateLocalPokemonCommand;
import com.tech.challenge.application.pokemon.result.LocalPokemonResult;

public interface UpdateLocalPokemonUseCase {

    LocalPokemonResult update(UpdateLocalPokemonCommand command);
}
