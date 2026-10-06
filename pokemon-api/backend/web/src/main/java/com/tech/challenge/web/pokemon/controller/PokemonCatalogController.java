package com.tech.challenge.web.pokemon.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tech.challenge.application.pokemon.port.in.GetPokemonDetailsUseCase;
import com.tech.challenge.application.pokemon.port.in.ListPokemonUseCase;
import com.tech.challenge.web.pokemon.dto.response.PokemonDetailsResponse;
import com.tech.challenge.web.pokemon.dto.response.PokemonSummaryResponse;
import com.tech.challenge.web.pokemon.mapper.PokemonDetailsResponseMapper;
import com.tech.challenge.web.shared.pagination.PageResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/pokemon")
@RequiredArgsConstructor
public class PokemonCatalogController {

    private final GetPokemonDetailsUseCase getPokemonDetailsUseCase;
    private final ListPokemonUseCase listPokemonUseCase;
    private final PokemonDetailsResponseMapper pokemonDetailsResponseMapper;

    @GetMapping
    public PageResponse<PokemonSummaryResponse> list(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "must be greater than or equal to 0") final int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "must be greater than or equal to 1")
            @Max(value = 100, message = "must be less than or equal to 100") final int size) {
        log.debug("Received request for Pokemon page {} (size {})", page, size);
        return pokemonDetailsResponseMapper.toResponse(listPokemonUseCase.list(page, size));
    }

    @GetMapping("/{idOrName}")
    public PokemonDetailsResponse getDetails(@PathVariable @NotBlank(message = "must not be blank") final String idOrName) {
        log.debug("Received request for Pokemon details '{}'", idOrName);
        return pokemonDetailsResponseMapper.toResponse(getPokemonDetailsUseCase.getDetails(idOrName));
    }
}
