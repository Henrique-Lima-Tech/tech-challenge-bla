package com.tech.challenge.infrastructure.pokemon.pokeapi.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code GET /evolution-chain/{id}}: only the fields listed in {@code docs/challenge/pokeapi.md}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PokeApiEvolutionChain(ChainLink chain) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChainLink(PokeApiNamedResource species, @JsonProperty("evolves_to") List<ChainLink> evolvesTo) {
    }
}
