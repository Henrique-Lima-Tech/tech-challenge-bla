package com.tech.challenge.application.pokemon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tech.challenge.application.pokemon.port.out.PokemonCatalogPort;
import com.tech.challenge.application.shared.pagination.PageResult;
import com.tech.challenge.domain.pokemon.model.PokemonSummary;
import com.tech.challenge.domain.shared.exception.ExternalServiceUnavailableException;

@ExtendWith(MockitoExtension.class)
class ListPokemonServiceTest {

    private static final PokemonSummary BULBASAUR = new PokemonSummary(1, "bulbasaur", "https://img/1.png",
            "Seed Pokémon", new BigDecimal("6.9"), List.of("overgrow", "chlorophyll"));

    @Mock
    private PokemonCatalogPort pokemonCatalogPort;

    @InjectMocks
    private ListPokemonService service;

    @Test
    void shouldReturnCatalogPageWhenPageIsRequested() {
        // given
        final var page = PageResult.of(List.of(BULBASAUR), 0, 20, 1302);
        when(pokemonCatalogPort.findSummaries(0, 20)).thenReturn(page);

        // when
        final var result = service.list(0, 20);

        // then
        assertThat(result).isEqualTo(page);
    }

    @Test
    void shouldPropagateUnavailableWhenCatalogCannotBeReached() {
        // given
        final var unavailable = new ExternalServiceUnavailableException("PokeAPI is unavailable", null);
        when(pokemonCatalogPort.findSummaries(0, 20)).thenThrow(unavailable);

        // when & then
        assertThatThrownBy(() -> service.list(0, 20)).isSameAs(unavailable);
    }
}
