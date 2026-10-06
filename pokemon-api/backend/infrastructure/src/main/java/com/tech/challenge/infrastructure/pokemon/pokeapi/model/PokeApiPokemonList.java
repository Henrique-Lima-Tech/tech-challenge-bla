package com.tech.challenge.infrastructure.pokemon.pokeapi.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PokeApiPokemonList(int count, List<PokeApiNamedResource> results) {
}
