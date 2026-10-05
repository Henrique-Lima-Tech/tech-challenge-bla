package com.tech.challenge.infrastructure.pokemon.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.tech.challenge.domain.pokemon.model.LocalPokemon;
import com.tech.challenge.infrastructure.pokemon.persistence.entity.LocalPokemonEntity;

@Mapper(componentModel = "spring")
public interface LocalPokemonPersistenceMapper {

    LocalPokemon toDomain(LocalPokemonEntity entity);

    @Mapping(target = "userId", ignore = true)
    LocalPokemonEntity toEntity(LocalPokemon pokemon);
}
