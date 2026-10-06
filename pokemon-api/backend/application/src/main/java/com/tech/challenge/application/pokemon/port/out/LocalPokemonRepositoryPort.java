package com.tech.challenge.application.pokemon.port.out;

import java.util.Optional;

import com.tech.challenge.application.shared.pagination.PageResult;
import com.tech.challenge.domain.pokemon.model.LocalPokemon;

public interface LocalPokemonRepositoryPort {

    LocalPokemon save(long userId, LocalPokemon pokemon);

    Optional<LocalPokemon> findById(long userId, long id);

    PageResult<LocalPokemon> findPage(long userId, int page, int size);

    boolean deleteById(long userId, long id);
}
