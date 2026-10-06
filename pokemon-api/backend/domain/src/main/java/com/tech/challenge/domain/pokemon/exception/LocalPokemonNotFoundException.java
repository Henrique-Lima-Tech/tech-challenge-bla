package com.tech.challenge.domain.pokemon.exception;

import lombok.Getter;

@Getter
public class LocalPokemonNotFoundException extends RuntimeException {

    private final long id;

    public LocalPokemonNotFoundException(final long id) {
        super("Local Pokemon not found: " + id);
        this.id = id;
    }
}
