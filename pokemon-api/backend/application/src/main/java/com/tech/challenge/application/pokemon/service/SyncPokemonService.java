package com.tech.challenge.application.pokemon.service;

import java.util.Locale;

import com.tech.challenge.application.pokemon.command.SyncPokemonCommand;
import com.tech.challenge.application.pokemon.port.in.SyncPokemonUseCase;
import com.tech.challenge.application.pokemon.port.out.LocalPokemonRepositoryPort;
import com.tech.challenge.application.pokemon.port.out.PokemonCatalogPort;
import com.tech.challenge.application.pokemon.result.LocalPokemonResult;
import com.tech.challenge.domain.pokemon.exception.PokemonNotFoundException;
import com.tech.challenge.domain.pokemon.model.LocalPokemon;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SyncPokemonService implements SyncPokemonUseCase {

    private final PokemonCatalogPort pokemonCatalogPort;
    private final LocalPokemonRepositoryPort localPokemonRepositoryPort;

    @Override
    public LocalPokemonResult sync(final SyncPokemonCommand command) {
        final var idOrName = command.pokemon().trim().toLowerCase(Locale.ROOT);
        final var summary = pokemonCatalogPort.findSummary(idOrName)
                .orElseThrow(() -> new PokemonNotFoundException(idOrName));
        final var pokemon = new LocalPokemon(null, summary.id(), summary.name(), summary.spriteUrl(),
                summary.category(), summary.weightKg(), summary.abilities(), command.localizedName(), command.region(),
                command.internalTags());
        return LocalPokemonResult.from(localPokemonRepositoryPort.save(command.userId(), pokemon));
    }
}
