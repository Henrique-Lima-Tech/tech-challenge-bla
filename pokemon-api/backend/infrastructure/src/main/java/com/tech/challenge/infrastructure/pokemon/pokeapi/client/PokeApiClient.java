package com.tech.challenge.infrastructure.pokemon.pokeapi.client;

import java.net.URI;
import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.tech.challenge.infrastructure.pokemon.pokeapi.model.PokeApiEvolutionChain;
import com.tech.challenge.infrastructure.pokemon.pokeapi.model.PokeApiPokemon;
import com.tech.challenge.infrastructure.pokemon.pokeapi.model.PokeApiPokemonList;
import com.tech.challenge.infrastructure.pokemon.pokeapi.model.PokeApiSpecies;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PokeApiClient {

    private static final String POKEMON_LIST_CACHE = "pokeapi-pokemon-list";
    private static final String POKEMON_CACHE = "pokeapi-pokemon";
    private static final String SPECIES_CACHE = "pokeapi-species";
    private static final String EVOLUTION_CHAIN_CACHE = "pokeapi-evolution-chain";

    private final RestClient pokeApiRestClient;

    @Cacheable(POKEMON_CACHE)
    public Optional<PokeApiPokemon> findPokemon(final String idOrName) {
        log.debug("Fetching Pokemon '{}' from PokeAPI", idOrName);
        try {
            return Optional.ofNullable(pokeApiRestClient.get()
                    .uri("/pokemon/{idOrName}/", idOrName)
                    .retrieve()
                    .body(PokeApiPokemon.class));
        } catch (final HttpClientErrorException.NotFound e) {
            log.debug("PokeAPI answered 404 for Pokemon '{}'", idOrName);
            return Optional.empty();
        }
    }

    @Cacheable(SPECIES_CACHE)
    public PokeApiSpecies getSpecies(final String url) {
        log.debug("Fetching species from PokeAPI: {}", url);
        return pokeApiRestClient.get().uri(URI.create(url)).retrieve().body(PokeApiSpecies.class);
    }

    @Cacheable(EVOLUTION_CHAIN_CACHE)
    public PokeApiEvolutionChain getEvolutionChain(final String url) {
        log.debug("Fetching evolution chain from PokeAPI: {}", url);
        return pokeApiRestClient.get().uri(URI.create(url)).retrieve().body(PokeApiEvolutionChain.class);
    }

    @Cacheable(POKEMON_LIST_CACHE)
    public PokeApiPokemonList getPokemonList(final long offset, final int limit) {
        log.debug("Fetching Pokemon list from PokeAPI (offset {}, limit {})", offset, limit);
        return pokeApiRestClient.get()
                .uri("/pokemon?offset={offset}&limit={limit}", offset, limit)
                .retrieve()
                .body(PokeApiPokemonList.class);
    }

    @Cacheable(POKEMON_CACHE)
    public PokeApiPokemon getPokemon(final String url) {
        log.debug("Fetching Pokemon from PokeAPI: {}", url);
        return pokeApiRestClient.get().uri(URI.create(url)).retrieve().body(PokeApiPokemon.class);
    }
}
