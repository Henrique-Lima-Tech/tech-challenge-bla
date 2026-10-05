package com.tech.challenge.application.pokemon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tech.challenge.application.pokemon.port.out.PokemonCatalogPort;
import com.tech.challenge.domain.pokemon.exception.PokemonNotFoundException;
import com.tech.challenge.domain.pokemon.model.EvolutionStage;
import com.tech.challenge.domain.pokemon.model.PokemonDetails;
import com.tech.challenge.domain.pokemon.model.PokemonStat;
import com.tech.challenge.domain.shared.exception.ExternalServiceUnavailableException;

@ExtendWith(MockitoExtension.class)
class GetPokemonDetailsServiceTest {

    private static final PokemonDetails BULBASAUR = new PokemonDetails(1, "bulbasaur", "https://img/1.png",
            List.of(new PokemonStat("hp", 45)), "A seed.", new EvolutionStage("bulbasaur", null, List.of()));

    @Mock
    private PokemonCatalogPort pokemonCatalogPort;

    @InjectMocks
    private GetPokemonDetailsService service;

    @Test
    void shouldReturnDetailsWhenPokemonExists() {
        // given
        when(pokemonCatalogPort.findDetails("bulbasaur")).thenReturn(Optional.of(BULBASAUR));

        // when
        final var details = service.getDetails("bulbasaur");

        // then
        assertThat(details).isEqualTo(BULBASAUR);
    }

    @Test
    void shouldTrimAndLowercaseIdOrNameWhenCallingCatalog() {
        // given
        when(pokemonCatalogPort.findDetails("bulbasaur")).thenReturn(Optional.of(BULBASAUR));

        // when
        service.getDetails("  BulbaSaur ");

        // then
        verify(pokemonCatalogPort).findDetails("bulbasaur");
    }

    @Test
    void shouldThrowNotFoundWhenCatalogHasNoPokemon() {
        // given
        when(pokemonCatalogPort.findDetails("missingno")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.getDetails("missingno"))
                .isInstanceOf(PokemonNotFoundException.class)
                .hasMessage("Pokemon not found: missingno");
    }

    @Test
    void shouldPropagateUnavailableWhenCatalogCannotBeReached() {
        // given
        final var unavailable = new ExternalServiceUnavailableException("PokeAPI is unavailable", null);
        when(pokemonCatalogPort.findDetails("1")).thenThrow(unavailable);

        // when & then
        assertThatThrownBy(() -> service.getDetails("1")).isSameAs(unavailable);
    }
}
