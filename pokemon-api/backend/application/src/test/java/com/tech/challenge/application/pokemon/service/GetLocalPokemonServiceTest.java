package com.tech.challenge.application.pokemon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tech.challenge.application.pokemon.port.out.LocalPokemonRepositoryPort;
import com.tech.challenge.application.pokemon.result.LocalPokemonResult;
import com.tech.challenge.domain.pokemon.exception.LocalPokemonNotFoundException;
import com.tech.challenge.domain.pokemon.model.LocalPokemon;

@ExtendWith(MockitoExtension.class)
class GetLocalPokemonServiceTest {

    private static final long USER_ID = 7L;

    private static final LocalPokemon STORED = new LocalPokemon(10L, 25, "pikachu", "https://img/25.png",
            "Mouse Pokémon", new BigDecimal("6.0"), List.of("static", "lightning-rod"), "ピカチュウ", "Kanto",
            List.of("starter", "electric"));

    @Mock
    private LocalPokemonRepositoryPort localPokemonRepositoryPort;

    @InjectMocks
    private GetLocalPokemonService service;

    @Test
    void shouldReturnStoredPokemonWhenIdExists() {
        // given
        when(localPokemonRepositoryPort.findById(USER_ID, 10L)).thenReturn(Optional.of(STORED));

        // when
        final var result = service.get(USER_ID, 10L);

        // then
        assertThat(result).isEqualTo(new LocalPokemonResult(10L, 25, "pikachu", "https://img/25.png",
                "Mouse Pokémon", new BigDecimal("6.0"), List.of("static", "lightning-rod"), "ピカチュウ", "Kanto",
                List.of("starter", "electric")));
    }

    @Test
    void shouldThrowNotFoundWhenUserHasNoCopyWithThatId() {
        // given
        when(localPokemonRepositoryPort.findById(USER_ID, 99L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.get(USER_ID, 99L))
                .isInstanceOf(LocalPokemonNotFoundException.class)
                .hasMessage("Local Pokemon not found: 99");
    }
}
