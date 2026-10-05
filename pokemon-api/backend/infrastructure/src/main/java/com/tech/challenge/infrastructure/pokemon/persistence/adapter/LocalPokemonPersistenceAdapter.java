package com.tech.challenge.infrastructure.pokemon.persistence.adapter;

import java.util.Locale;
import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tech.challenge.application.pokemon.port.out.LocalPokemonRepositoryPort;
import com.tech.challenge.application.shared.pagination.PageResult;
import com.tech.challenge.domain.pokemon.exception.PokemonAlreadySyncedException;
import com.tech.challenge.domain.pokemon.model.LocalPokemon;
import com.tech.challenge.infrastructure.pokemon.persistence.mapper.LocalPokemonPersistenceMapper;
import com.tech.challenge.infrastructure.pokemon.persistence.repository.LocalPokemonJpaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalPokemonPersistenceAdapter implements LocalPokemonRepositoryPort {

    /** Name given in {@code V3__add_owner_to_local_pokemon.sql}. */
    private static final String POKE_API_ID_CONSTRAINT = "uk_local_pokemon_user_poke_api_id";

    private final LocalPokemonJpaRepository localPokemonJpaRepository;
    private final LocalPokemonPersistenceMapper localPokemonPersistenceMapper;

    @Override
    public LocalPokemon save(final long userId, final LocalPokemon pokemon) {
        final var entity = localPokemonPersistenceMapper.toEntity(pokemon);
        entity.setUserId(userId);
        try {
            final var saved = localPokemonPersistenceMapper.toDomain(localPokemonJpaRepository.saveAndFlush(entity));
            log.debug("Saved local Pokemon {} (pokeApiId {}) for user {}", saved.id(), saved.pokeApiId(), userId);
            return saved;
        } catch (final DataIntegrityViolationException e) {
            if (!isPokeApiIdConstraint(e)) {
                throw e;
            }
            // The cause is left out on purpose: the database message contains the rejected values.
            log.warn("Saving local Pokemon failed: pokeApiId {} already synced by user {}", pokemon.pokeApiId(),
                    userId);
            throw new PokemonAlreadySyncedException();
        }
    }

    /** The lists are lazy {@code @ElementCollection}s: the mapper reads them inside this transaction. */
    @Override
    @Transactional(readOnly = true)
    public Optional<LocalPokemon> findById(final long userId, final long id) {
        return localPokemonJpaRepository.findByIdAndUserId(id, userId).map(localPokemonPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<LocalPokemon> findPage(final long userId, final int page, final int size) {
        final var stored = localPokemonJpaRepository.findAllByUserId(userId, PageRequest.of(page, size, Sort.by("id")));
        return PageResult.of(stored.getContent().stream().map(localPokemonPersistenceMapper::toDomain).toList(), page,
                size, stored.getTotalElements());
    }

    @Override
    @Transactional
    public boolean deleteById(final long userId, final long id) {
        if (!localPokemonJpaRepository.existsByIdAndUserId(id, userId)) {
            return false;
        }
        localPokemonJpaRepository.deleteById(id);
        log.debug("Deleted local Pokemon {} of user {}", id, userId);
        return true;
    }

    private static boolean isPokeApiIdConstraint(final DataIntegrityViolationException e) {
        return e.getCause() instanceof final ConstraintViolationException violation
                && violation.getConstraintName() != null
                && violation.getConstraintName().toLowerCase(Locale.ROOT).contains(POKE_API_ID_CONSTRAINT);
    }
}
