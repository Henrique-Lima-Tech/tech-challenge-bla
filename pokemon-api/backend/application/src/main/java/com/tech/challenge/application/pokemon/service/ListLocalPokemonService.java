package com.tech.challenge.application.pokemon.service;

import com.tech.challenge.application.pokemon.port.in.ListLocalPokemonUseCase;
import com.tech.challenge.application.pokemon.port.out.LocalPokemonRepositoryPort;
import com.tech.challenge.application.pokemon.result.LocalPokemonResult;
import com.tech.challenge.application.shared.pagination.PageResult;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListLocalPokemonService implements ListLocalPokemonUseCase {

    private final LocalPokemonRepositoryPort localPokemonRepositoryPort;

    @Override
    public PageResult<LocalPokemonResult> list(final long userId, final int page, final int size) {
        final var stored = localPokemonRepositoryPort.findPage(userId, page, size);
        return PageResult.of(stored.content().stream().map(LocalPokemonResult::from).toList(), stored.page(),
                stored.size(), stored.totalElements());
    }
}
