package com.tech.challenge.infrastructure.pokemon.pokeapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * PokéAPI {@code NamedAPIResource}; also used for the unnamed {@code APIResource} (then {@code name} is null).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PokeApiNamedResource(String name, String url) {
}
