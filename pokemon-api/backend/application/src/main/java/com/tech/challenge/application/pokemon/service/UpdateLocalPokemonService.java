package com.tech.challenge.application.pokemon.service;

import com.tech.challenge.application.pokemon.command.UpdateLocalPokemonCommand;
import com.tech.challenge.application.pokemon.port.in.UpdateLocalPokemonUseCase;
import com.tech.challenge.application.pokemon.port.out.LocalPokemonRepositoryPort;
import com.tech.challenge.application.pokemon.result.LocalPokemonResult;
import com.tech.challenge.domain.pokemon.exception.LocalPokemonNotFoundException;
import com.tech.challenge.domain.pokemon.model.LocalPokemon;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateLocalPokemonService implements UpdateLocalPokemonUseCase {

    private final LocalPokemonRepositoryPort localPokemonRepositoryPort;

    @Override
    public LocalPokemonResult update(final UpdateLocalPokemonCommand command) {
        final var stored = localPokemonRepositoryPort.findById(command.userId(), command.id())
                .orElseThrow(() -> new LocalPokemonNotFoundException(command.id()));
        final var updated = new LocalPokemon(stored.id(), stored.pokeApiId(), command.name(), command.spriteUrl(),
                command.category(), command.weightKg(), command.abilities(), command.localizedName(),
                command.region(), command.internalTags());
        return LocalPokemonResult.from(localPokemonRepositoryPort.save(command.userId(), updated));
    }
}
