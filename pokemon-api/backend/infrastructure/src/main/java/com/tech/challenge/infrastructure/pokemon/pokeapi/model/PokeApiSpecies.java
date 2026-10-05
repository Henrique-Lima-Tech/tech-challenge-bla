package com.tech.challenge.infrastructure.pokemon.pokeapi.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code GET /pokemon-species/{id or name}}: only the fields listed in {@code docs/challenge/pokeapi.md}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PokeApiSpecies(
        @JsonProperty("flavor_text_entries") List<FlavorTextEntry> flavorTextEntries,
        @JsonProperty("evolution_chain") PokeApiNamedResource evolutionChain,
        List<Genus> genera) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FlavorTextEntry(@JsonProperty("flavor_text") String flavorText, PokeApiNamedResource language) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Genus(String genus, PokeApiNamedResource language) {
    }
}
