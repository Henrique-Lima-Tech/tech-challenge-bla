package com.tech.challenge.application.pokemon.service;

import java.util.Locale;

import com.tech.challenge.application.pokemon.port.in.GetPokemonDetailsUseCase;
import com.tech.challenge.application.pokemon.port.out.PokemonCatalogPort;
import com.tech.challenge.domain.pokemon.exception.PokemonNotFoundException;
import com.tech.challenge.domain.pokemon.model.PokemonDetails;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetPokemonDetailsService implements GetPokemonDetailsUseCase {

    private final PokemonCatalogPort pokemonCatalogPort;

    @Override
    public PokemonDetails getDetails(final String idOrName) {
        final var normalized = idOrName.trim().toLowerCase(Locale.ROOT);
        return pokemonCatalogPort.findDetails(normalized)
                .orElseThrow(() -> new PokemonNotFoundException(normalized));
    }
}
