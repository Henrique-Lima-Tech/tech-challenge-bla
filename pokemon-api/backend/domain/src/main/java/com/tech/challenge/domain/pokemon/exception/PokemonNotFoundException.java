package com.tech.challenge.domain.pokemon.exception;

import lombok.Getter;

@Getter
public class PokemonNotFoundException extends RuntimeException {

    private final String idOrName;

    public PokemonNotFoundException(final String idOrName) {
        super("Pokemon not found: " + idOrName);
        this.idOrName = idOrName;
    }
}
