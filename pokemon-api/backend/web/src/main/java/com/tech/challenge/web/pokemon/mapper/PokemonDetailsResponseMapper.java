package com.tech.challenge.web.pokemon.mapper;

import org.mapstruct.Mapper;

import com.tech.challenge.application.shared.pagination.PageResult;
import com.tech.challenge.domain.pokemon.model.EvolutionStage;
import com.tech.challenge.domain.pokemon.model.PokemonDetails;
import com.tech.challenge.domain.pokemon.model.PokemonStat;
import com.tech.challenge.domain.pokemon.model.PokemonSummary;
import com.tech.challenge.web.pokemon.dto.response.EvolutionStageResponse;
import com.tech.challenge.web.pokemon.dto.response.PokemonDetailsResponse;
import com.tech.challenge.web.pokemon.dto.response.PokemonSummaryResponse;
import com.tech.challenge.web.pokemon.dto.response.StatResponse;
import com.tech.challenge.web.shared.pagination.PageResponse;

@Mapper(componentModel = "spring")
public interface PokemonDetailsResponseMapper {

    PokemonDetailsResponse toResponse(PokemonDetails details);

    StatResponse toResponse(PokemonStat stat);

    EvolutionStageResponse toResponse(EvolutionStage stage);

    PageResponse<PokemonSummaryResponse> toResponse(PageResult<PokemonSummary> page);

    PokemonSummaryResponse toResponse(PokemonSummary summary);
}
