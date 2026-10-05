package com.tech.challenge.domain.pokemon.model;

public record PokemonStat(String name, int baseStat) {

    public PokemonStat {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Stat name must not be blank");
        }
        if (baseStat < 0) {
            throw new IllegalArgumentException("Base stat must not be negative");
        }
    }
}
