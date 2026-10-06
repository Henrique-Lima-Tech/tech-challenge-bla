package com.tech.challenge.infrastructure.pokemon.pokeapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PokeApiNamedResource(String name, String url) {
}
