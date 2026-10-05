package com.tech.challenge.application.pokemon.port.in;

import com.tech.challenge.application.shared.pagination.PageResult;
import com.tech.challenge.domain.pokemon.model.PokemonSummary;

/**
 * REQ-US01: browse Pokémon via paginated results.
 */
public interface ListPokemonUseCase {

    /**
     * @param page 0-based page number
     * @throws com.tech.challenge.domain.shared.exception.ExternalServiceUnavailableException if the PokéAPI cannot answer
     */
    PageResult<PokemonSummary> list(int page, int size);
}
