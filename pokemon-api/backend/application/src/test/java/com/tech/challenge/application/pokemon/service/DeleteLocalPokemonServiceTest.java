package com.tech.challenge.application.pokemon.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tech.challenge.application.pokemon.port.out.LocalPokemonRepositoryPort;
import com.tech.challenge.domain.pokemon.exception.LocalPokemonNotFoundException;

@ExtendWith(MockitoExtension.class)
class DeleteLocalPokemonServiceTest {

    private static final long USER_ID = 7L;

    @Mock
    private LocalPokemonRepositoryPort localPokemonRepositoryPort;

    @InjectMocks
    private DeleteLocalPokemonService service;

    @Test
    void shouldDeleteStoredPokemonWhenIdExists() {
        // given
        when(localPokemonRepositoryPort.deleteById(USER_ID, 10L)).thenReturn(true);

        // when
        service.delete(USER_ID, 10L);

        // then
        verify(localPokemonRepositoryPort).deleteById(USER_ID, 10L);
    }

    @Test
    void shouldThrowNotFoundWhenUserHasNoCopyWithThatId() {
        // given
        when(localPokemonRepositoryPort.deleteById(USER_ID, 99L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> service.delete(USER_ID, 99L))
                .isInstanceOf(LocalPokemonNotFoundException.class)
                .hasMessage("Local Pokemon not found: 99");
    }
}
