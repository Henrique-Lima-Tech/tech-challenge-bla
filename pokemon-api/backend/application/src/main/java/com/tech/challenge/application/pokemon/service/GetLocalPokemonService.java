package com.tech.challenge.application.pokemon.service;

import com.tech.challenge.application.pokemon.port.in.GetLocalPokemonUseCase;
import com.tech.challenge.application.pokemon.port.out.LocalPokemonRepositoryPort;
import com.tech.challenge.application.pokemon.result.LocalPokemonResult;
import com.tech.challenge.domain.pokemon.exception.LocalPokemonNotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetLocalPokemonService implements GetLocalPokemonUseCase {

    private final LocalPokemonRepositoryPort localPokemonRepositoryPort;

    @Override
    public LocalPokemonResult get(final long userId, final long id) {
        return localPokemonRepositoryPort.findById(userId, id)
                .map(LocalPokemonResult::from)
                .orElseThrow(() -> new LocalPokemonNotFoundException(id));
    }
}
