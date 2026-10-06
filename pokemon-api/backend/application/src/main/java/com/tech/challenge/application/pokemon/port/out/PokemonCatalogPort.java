package com.tech.challenge.application.pokemon.port.out;

import java.util.Optional;

import com.tech.challenge.application.shared.pagination.PageResult;
import com.tech.challenge.domain.pokemon.model.PokemonDetails;
import com.tech.challenge.domain.pokemon.model.PokemonSummary;

public interface PokemonCatalogPort {

    Optional<PokemonDetails> findDetails(String idOrName);

    Optional<PokemonSummary> findSummary(String idOrName);

    PageResult<PokemonSummary> findSummaries(int page, int size);
}
