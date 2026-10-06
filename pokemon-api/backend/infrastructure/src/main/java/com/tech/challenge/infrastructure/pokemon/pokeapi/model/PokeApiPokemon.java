package com.tech.challenge.infrastructure.pokemon.pokeapi.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PokeApiPokemon(
        int id,
        String name,
        Sprites sprites,
        List<StatSlot> stats,
        PokeApiNamedResource species,
        int weight,
        List<AbilitySlot> abilities) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Sprites(@JsonProperty("front_default") String frontDefault) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatSlot(@JsonProperty("base_stat") int baseStat, PokeApiNamedResource stat) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AbilitySlot(PokeApiNamedResource ability) {
    }
}
