package com.tech.challenge.application.pokemon.port.in;

import com.tech.challenge.application.pokemon.command.UpdateLocalPokemonCommand;
import com.tech.challenge.application.pokemon.result.LocalPokemonResult;

/**
 * REQ-US04: replaces every field of a local copy except its identifiers (D-25).
 */
public interface UpdateLocalPokemonUseCase {

    /**
     * @throws com.tech.challenge.domain.pokemon.exception.LocalPokemonNotFoundException if the user has no copy with
     *         that id
     * @throws IllegalArgumentException if a value breaks a {@code LocalPokemon} invariant
     */
    LocalPokemonResult update(UpdateLocalPokemonCommand command);
}
