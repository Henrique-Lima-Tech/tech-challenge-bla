package com.tech.challenge.domain.pokemon.exception;

public class PokemonAlreadySyncedException extends RuntimeException {

    public PokemonAlreadySyncedException() {
        super("Pokemon already synced");
    }
}
