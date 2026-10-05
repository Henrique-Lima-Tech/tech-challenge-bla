package com.tech.challenge.application.pokemon.port.in;

/**
 * REQ-API01: delete one of the user's local copies (D-31).
 */
public interface DeleteLocalPokemonUseCase {

    /**
     * @throws com.tech.challenge.domain.pokemon.exception.LocalPokemonNotFoundException if the user has no copy with
     *         that id
     */
    void delete(long userId, long id);
}
