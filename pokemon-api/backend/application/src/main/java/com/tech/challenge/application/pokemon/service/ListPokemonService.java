package com.tech.challenge.application.pokemon.service;

import com.tech.challenge.application.pokemon.port.in.ListPokemonUseCase;
import com.tech.challenge.application.pokemon.port.out.PokemonCatalogPort;
import com.tech.challenge.application.shared.pagination.PageResult;
import com.tech.challenge.domain.pokemon.model.PokemonSummary;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListPokemonService implements ListPokemonUseCase {

    private final PokemonCatalogPort pokemonCatalogPort;

    @Override
    public PageResult<PokemonSummary> list(final int page, final int size) {
        return pokemonCatalogPort.findSummaries(page, size);
    }
}
