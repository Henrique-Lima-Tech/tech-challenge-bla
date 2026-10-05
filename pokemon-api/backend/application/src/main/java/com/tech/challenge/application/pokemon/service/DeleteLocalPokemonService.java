package com.tech.challenge.application.pokemon.service;

import com.tech.challenge.application.pokemon.port.in.DeleteLocalPokemonUseCase;
import com.tech.challenge.application.pokemon.port.out.LocalPokemonRepositoryPort;
import com.tech.challenge.domain.pokemon.exception.LocalPokemonNotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeleteLocalPokemonService implements DeleteLocalPokemonUseCase {

    private final LocalPokemonRepositoryPort localPokemonRepositoryPort;

    @Override
    public void delete(final long userId, final long id) {
        if (!localPokemonRepositoryPort.deleteById(userId, id)) {
            throw new LocalPokemonNotFoundException(id);
        }
    }
}
