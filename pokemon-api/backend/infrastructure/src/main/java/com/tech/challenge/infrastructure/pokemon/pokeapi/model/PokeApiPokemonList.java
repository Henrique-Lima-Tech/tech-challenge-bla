package com.tech.challenge.infrastructure.pokemon.pokeapi.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * {@code GET /pokemon?limit={n}&offset={m}} ({@code NamedAPIResourceList}): only the fields listed in
 * {@code docs/challenge/pokeapi.md} that the code reads.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PokeApiPokemonList(int count, List<PokeApiNamedResource> results) {
}
