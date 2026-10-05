package com.tech.challenge.application.pokemon.port.out;

import java.util.Optional;

import com.tech.challenge.application.shared.pagination.PageResult;
import com.tech.challenge.domain.pokemon.model.LocalPokemon;

/**
 * The local copies of Pokémon (D-09). Each copy belongs to one user, and every method works on that user's
 * copies only (D-31).
 */
public interface LocalPokemonRepositoryPort {

    /**
     * Stores a new copy for the user, or replaces every field of the stored one when {@code pokemon} already has an
     * id (D-25).
     *
     * @throws com.tech.challenge.domain.pokemon.exception.PokemonAlreadySyncedException if the user already has a copy
     *         with the same {@code pokeApiId}
     */
    LocalPokemon save(long userId, LocalPokemon pokemon);

    /**
     * @return empty if the user has no copy with that id
     */
    Optional<LocalPokemon> findById(long userId, long id);

    /**
     * @param page 0-based page number
     * @return the user's copies sorted by id ascending; a page past the end has no content and the real totals
     */
    PageResult<LocalPokemon> findPage(long userId, int page, int size);

    /**
     * Removes the user's copy with that id, with its abilities and internal tags.
     *
     * @return {@code false} if the user had no copy with that id
     */
    boolean deleteById(long userId, long id);
}
