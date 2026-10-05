package com.tech.challenge.domain.pokemon.exception;

import lombok.Getter;

/**
 * No local copy with that id is stored (REQ-US04). Unlike {@link PokemonNotFoundException}, it says nothing
 * about what the PokéAPI knows.
 */
@Getter
public class LocalPokemonNotFoundException extends RuntimeException {

    private final long id;

    public LocalPokemonNotFoundException(final long id) {
        super("Local Pokemon not found: " + id);
        this.id = id;
    }
}
