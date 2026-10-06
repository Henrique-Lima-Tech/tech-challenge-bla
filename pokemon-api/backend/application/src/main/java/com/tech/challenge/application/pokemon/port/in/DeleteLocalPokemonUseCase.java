package com.tech.challenge.application.pokemon.port.in;

public interface DeleteLocalPokemonUseCase {

    void delete(long userId, long id);
}
