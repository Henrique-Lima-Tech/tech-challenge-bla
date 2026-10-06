package com.tech.challenge.application.pokemon.port.in;

import com.tech.challenge.application.pokemon.command.SyncPokemonCommand;
import com.tech.challenge.application.pokemon.result.LocalPokemonResult;

public interface SyncPokemonUseCase {

    LocalPokemonResult sync(SyncPokemonCommand command);
}
