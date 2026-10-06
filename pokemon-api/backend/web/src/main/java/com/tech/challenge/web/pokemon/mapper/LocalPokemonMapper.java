package com.tech.challenge.web.pokemon.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.tech.challenge.application.pokemon.command.SyncPokemonCommand;
import com.tech.challenge.application.pokemon.command.UpdateLocalPokemonCommand;
import com.tech.challenge.application.pokemon.result.LocalPokemonResult;
import com.tech.challenge.application.shared.pagination.PageResult;
import com.tech.challenge.web.pokemon.dto.request.SyncPokemonRequest;
import com.tech.challenge.web.pokemon.dto.request.UpdateLocalPokemonRequest;
import com.tech.challenge.web.pokemon.dto.response.LocalPokemonResponse;
import com.tech.challenge.web.shared.pagination.PageResponse;

@Mapper(componentModel = "spring")
public interface LocalPokemonMapper {

    SyncPokemonCommand toCommand(long userId, SyncPokemonRequest request);

    @Mapping(target = "id", source = "pokemonId")
    UpdateLocalPokemonCommand toCommand(long userId, long pokemonId, UpdateLocalPokemonRequest request);

    LocalPokemonResponse toResponse(LocalPokemonResult result);

    PageResponse<LocalPokemonResponse> toResponse(PageResult<LocalPokemonResult> page);
}
