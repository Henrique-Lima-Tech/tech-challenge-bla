package com.tech.challenge.application.pokemon.port.in;

import com.tech.challenge.application.shared.pagination.PageResult;
import com.tech.challenge.domain.pokemon.model.PokemonSummary;

public interface ListPokemonUseCase {

    PageResult<PokemonSummary> list(int page, int size);
}
