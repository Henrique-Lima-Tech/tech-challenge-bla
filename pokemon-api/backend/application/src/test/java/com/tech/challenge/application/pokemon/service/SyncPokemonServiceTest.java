package com.tech.challenge.application.pokemon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tech.challenge.application.pokemon.command.SyncPokemonCommand;
import com.tech.challenge.application.pokemon.port.out.LocalPokemonRepositoryPort;
import com.tech.challenge.application.pokemon.port.out.PokemonCatalogPort;
import com.tech.challenge.application.pokemon.result.LocalPokemonResult;
import com.tech.challenge.domain.pokemon.exception.PokemonAlreadySyncedException;
import com.tech.challenge.domain.pokemon.exception.PokemonNotFoundException;
import com.tech.challenge.domain.pokemon.model.LocalPokemon;
import com.tech.challenge.domain.pokemon.model.PokemonSummary;
import com.tech.challenge.domain.shared.exception.ExternalServiceUnavailableException;

@ExtendWith(MockitoExtension.class)
class SyncPokemonServiceTest {

    private static final long USER_ID = 7L;
    private static final BigDecimal WEIGHT = new BigDecimal("6.0");
    private static final List<String> ABILITIES = List.of("static", "lightning-rod");
    private static final PokemonSummary PIKACHU = new PokemonSummary(25, "pikachu", "https://img/25.png",
            "Mouse Pokémon", WEIGHT, ABILITIES);

    @Mock
    private PokemonCatalogPort pokemonCatalogPort;

    @Mock
    private LocalPokemonRepositoryPort localPokemonRepositoryPort;

    @InjectMocks
    private SyncPokemonService service;

    @Test
    void shouldStoreCatalogDataWithProprietaryFieldsForTheUserWhenPokemonExists() {
        // given
        final var toSave = new LocalPokemon(null, 25, "pikachu", "https://img/25.png", "Mouse Pokémon", WEIGHT,
                ABILITIES, "ピカチュウ", "Kanto", List.of("starter", "electric"));
        final var saved = new LocalPokemon(10L, 25, "pikachu", "https://img/25.png", "Mouse Pokémon", WEIGHT,
                ABILITIES, "ピカチュウ", "Kanto", List.of("starter", "electric"));
        when(pokemonCatalogPort.findSummary("pikachu")).thenReturn(Optional.of(PIKACHU));
        when(localPokemonRepositoryPort.save(USER_ID, toSave)).thenReturn(saved);

        // when
        final var result = service.sync(new SyncPokemonCommand(USER_ID, "pikachu", "ピカチュウ", "Kanto",
                List.of("starter", "electric")));

        // then
        assertThat(result).isEqualTo(new LocalPokemonResult(10L, 25, "pikachu", "https://img/25.png", "Mouse Pokémon",
                WEIGHT, ABILITIES, "ピカチュウ", "Kanto", List.of("starter", "electric")));
    }

    @Test
    void shouldTrimAndLowercasePokemonWhenCallingCatalog() {
        // given
        when(pokemonCatalogPort.findSummary("pikachu")).thenReturn(Optional.of(PIKACHU));
        when(localPokemonRepositoryPort.save(eq(USER_ID), any()))
                .thenAnswer(invocation -> withId(invocation.getArgument(1)));

        // when
        service.sync(new SyncPokemonCommand(USER_ID, "  Pikachu ", null, null, null));

        // then
        verify(pokemonCatalogPort).findSummary("pikachu");
    }

    @Test
    void shouldStoreNullProprietaryFieldsAndNoTagsWhenTheyAreAbsent() {
        // given
        when(pokemonCatalogPort.findSummary("25")).thenReturn(Optional.of(PIKACHU));
        when(localPokemonRepositoryPort.save(eq(USER_ID), any()))
                .thenAnswer(invocation -> withId(invocation.getArgument(1)));

        // when
        final var result = service.sync(new SyncPokemonCommand(USER_ID, "25", null, null, null));

        // then
        verify(localPokemonRepositoryPort).save(eq(USER_ID), eq(new LocalPokemon(null, 25, "pikachu",
                "https://img/25.png", "Mouse Pokémon", WEIGHT, ABILITIES, null, null, List.of())));
        assertThat(result.localizedName()).isNull();
        assertThat(result.region()).isNull();
        assertThat(result.internalTags()).isEmpty();
    }

    @Test
    void shouldThrowNotFoundWithoutSavingWhenCatalogHasNoPokemon() {
        // given
        when(pokemonCatalogPort.findSummary("missingno")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.sync(new SyncPokemonCommand(USER_ID, "MissingNo", null, null, null)))
                .isInstanceOf(PokemonNotFoundException.class)
                .hasMessage("Pokemon not found: missingno");

        verify(localPokemonRepositoryPort, never()).save(anyLong(), any());
    }

    @Test
    void shouldPropagateUnavailableWithoutSavingWhenCatalogCannotBeReached() {
        // given
        final var unavailable = new ExternalServiceUnavailableException("PokeAPI is unavailable", null);
        when(pokemonCatalogPort.findSummary("pikachu")).thenThrow(unavailable);

        // when & then
        assertThatThrownBy(() -> service.sync(new SyncPokemonCommand(USER_ID, "pikachu", null, null, null)))
                .isSameAs(unavailable);

        verify(localPokemonRepositoryPort, never()).save(anyLong(), any());
    }

    @Test
    void shouldPropagateAlreadySyncedWhenRepositoryRejectsDuplicate() {
        // given
        final var duplicate = new PokemonAlreadySyncedException();
        when(pokemonCatalogPort.findSummary("pikachu")).thenReturn(Optional.of(PIKACHU));
        when(localPokemonRepositoryPort.save(eq(USER_ID), any())).thenThrow(duplicate);

        // when & then
        assertThatThrownBy(() -> service.sync(new SyncPokemonCommand(USER_ID, "pikachu", null, null, null)))
                .isSameAs(duplicate);
    }

    private static LocalPokemon withId(final LocalPokemon pokemon) {
        return new LocalPokemon(10L, pokemon.pokeApiId(), pokemon.name(), pokemon.spriteUrl(), pokemon.category(),
                pokemon.weightKg(), pokemon.abilities(), pokemon.localizedName(), pokemon.region(),
                pokemon.internalTags());
    }
}
