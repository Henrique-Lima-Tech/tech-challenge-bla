package com.tech.challenge.application.pokemon.port.out;

import java.util.Optional;

import com.tech.challenge.application.shared.pagination.PageResult;
import com.tech.challenge.domain.pokemon.model.PokemonDetails;
import com.tech.challenge.domain.pokemon.model.PokemonSummary;

/**
 * The Pokémon catalog (the PokéAPI, D-09).
 */
public interface PokemonCatalogPort {

    /**
     * @return empty if the catalog does not know the Pokémon
     * @throws com.tech.challenge.domain.shared.exception.ExternalServiceUnavailableException if the catalog cannot answer
     */
    Optional<PokemonDetails> findDetails(String idOrName);

    /**
     * The US01 data (sprite, category, weight, abilities) of one Pokémon.
     *
     * @return empty if the catalog does not know the Pokémon
     * @throws com.tech.challenge.domain.shared.exception.ExternalServiceUnavailableException if the catalog cannot answer
     */
    Optional<PokemonSummary> findSummary(String idOrName);

    /**
     * @param page 0-based page number
     * @throws com.tech.challenge.domain.shared.exception.ExternalServiceUnavailableException if the catalog cannot answer
     */
    PageResult<PokemonSummary> findSummaries(int page, int size);
}
