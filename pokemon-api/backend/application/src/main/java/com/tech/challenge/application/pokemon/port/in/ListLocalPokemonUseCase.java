package com.tech.challenge.application.pokemon.port.in;

import com.tech.challenge.application.pokemon.result.LocalPokemonResult;
import com.tech.challenge.application.shared.pagination.PageResult;

public interface ListLocalPokemonUseCase {

    PageResult<LocalPokemonResult> list(long userId, int page, int size);
}
