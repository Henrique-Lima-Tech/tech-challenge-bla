package com.tech.challenge.infrastructure.pokemon.persistence.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tech.challenge.infrastructure.pokemon.persistence.entity.LocalPokemonEntity;

public interface LocalPokemonJpaRepository extends JpaRepository<LocalPokemonEntity, Long> {

    Optional<LocalPokemonEntity> findByIdAndUserId(Long id, Long userId);

    Page<LocalPokemonEntity> findAllByUserId(Long userId, Pageable pageable);

    boolean existsByIdAndUserId(Long id, Long userId);
}
