package com.tech.challenge.infrastructure.pokemon.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.tech.challenge.domain.pokemon.model.LocalPokemon;
import com.tech.challenge.infrastructure.pokemon.persistence.entity.LocalPokemonEntity;
import com.tech.challenge.infrastructure.pokemon.persistence.mapper.LocalPokemonPersistenceMapper;
import com.tech.challenge.infrastructure.pokemon.persistence.repository.LocalPokemonJpaRepository;

@ExtendWith(MockitoExtension.class)
class LocalPokemonPersistenceAdapterTranslationTest {

    private static final LocalPokemon PIKACHU = new LocalPokemon(null, 25, "pikachu", null, null,
            new BigDecimal("6.0"), List.of("static"), null, null, null);

    @Mock
    private LocalPokemonJpaRepository localPokemonJpaRepository;

    @Mock
    private LocalPokemonPersistenceMapper localPokemonPersistenceMapper;

    @InjectMocks
    private LocalPokemonPersistenceAdapter adapter;

    @Test
    void shouldRethrowWhenIntegrityViolationIsNotThePokeApiIdConstraint() {
        // given
        final var violation = new DataIntegrityViolationException("other",
                new ConstraintViolationException("other", null, "FK_LOCAL_POKEMON_ABILITIES_LOCAL_POKEMON"));
        when(localPokemonPersistenceMapper.toEntity(PIKACHU)).thenReturn(new LocalPokemonEntity());
        when(localPokemonJpaRepository.saveAndFlush(any())).thenThrow(violation);

        // when & then
        assertThatThrownBy(() -> adapter.save(7L, PIKACHU)).isSameAs(violation);
    }

    @Test
    void shouldRethrowWhenIntegrityViolationHasNoConstraintName() {
        // given
        final var violation = new DataIntegrityViolationException("other");
        when(localPokemonPersistenceMapper.toEntity(PIKACHU)).thenReturn(new LocalPokemonEntity());
        when(localPokemonJpaRepository.saveAndFlush(any())).thenThrow(violation);

        // when & then
        assertThatThrownBy(() -> adapter.save(7L, PIKACHU)).isSameAs(violation);
    }
}
