package com.tech.challenge.application.pokemon.port.in;

import com.tech.challenge.application.pokemon.result.LocalPokemonResult;

/**
 * REQ-API01: read one of the user's local copies (D-31).
 */
public interface GetLocalPokemonUseCase {

    /**
     * @throws com.tech.challenge.domain.pokemon.exception.LocalPokemonNotFoundException if the user has no copy with
     *         that id
     */
    LocalPokemonResult get(long userId, long id);
}
