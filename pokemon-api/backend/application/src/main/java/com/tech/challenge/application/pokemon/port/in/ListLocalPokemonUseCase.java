package com.tech.challenge.application.pokemon.port.in;

import com.tech.challenge.application.pokemon.result.LocalPokemonResult;
import com.tech.challenge.application.shared.pagination.PageResult;

/**
 * REQ-API01: list the user's local copies, sorted by id (D-31).
 */
public interface ListLocalPokemonUseCase {

    /**
     * @param page 0-based page number
     */
    PageResult<LocalPokemonResult> list(long userId, int page, int size);
}
