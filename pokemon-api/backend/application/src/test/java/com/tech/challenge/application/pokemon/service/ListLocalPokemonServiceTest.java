package com.tech.challenge.application.pokemon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tech.challenge.application.pokemon.port.out.LocalPokemonRepositoryPort;
import com.tech.challenge.application.pokemon.result.LocalPokemonResult;
import com.tech.challenge.application.shared.pagination.PageResult;
import com.tech.challenge.domain.pokemon.model.LocalPokemon;

@ExtendWith(MockitoExtension.class)
class ListLocalPokemonServiceTest {

    private static final long USER_ID = 7L;

    private static final LocalPokemon PIKACHU = new LocalPokemon(10L, 25, "pikachu", "https://img/25.png",
            "Mouse Pokémon", new BigDecimal("6.0"), List.of("static"), "ピカチュウ", "Kanto", List.of("starter"));

    @Mock
    private LocalPokemonRepositoryPort localPokemonRepositoryPort;

    @InjectMocks
    private ListLocalPokemonService service;

    @Test
    void shouldMapEveryStoredPokemonKeepingPaginationWhenPageHasContent() {
        // given
        when(localPokemonRepositoryPort.findPage(USER_ID, 1, 2)).thenReturn(PageResult.of(List.of(PIKACHU), 1, 2, 3));

        // when
        final var result = service.list(USER_ID, 1, 2);

        // then
        assertThat(result.content()).containsExactly(new LocalPokemonResult(10L, 25, "pikachu",
                "https://img/25.png", "Mouse Pokémon", new BigDecimal("6.0"), List.of("static"), "ピカチュウ", "Kanto",
                List.of("starter")));
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test
    void shouldReturnEmptyContentWithTotalsWhenPageIsPastTheEnd() {
        // given
        when(localPokemonRepositoryPort.findPage(USER_ID, 50, 20)).thenReturn(PageResult.of(List.of(), 50, 20, 3));

        // when
        final var result = service.list(USER_ID, 50, 20);

        // then
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(1);
    }
}
