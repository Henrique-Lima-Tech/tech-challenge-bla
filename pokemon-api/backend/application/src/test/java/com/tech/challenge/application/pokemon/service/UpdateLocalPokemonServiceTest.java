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

import com.tech.challenge.application.pokemon.command.UpdateLocalPokemonCommand;
import com.tech.challenge.application.pokemon.port.out.LocalPokemonRepositoryPort;
import com.tech.challenge.application.pokemon.result.LocalPokemonResult;
import com.tech.challenge.domain.pokemon.exception.LocalPokemonNotFoundException;
import com.tech.challenge.domain.pokemon.model.LocalPokemon;

@ExtendWith(MockitoExtension.class)
class UpdateLocalPokemonServiceTest {

    private static final long USER_ID = 7L;

    private static final LocalPokemon STORED = new LocalPokemon(10L, 25, "pikachu", "https://img/25.png",
            "Mouse Pokémon", new BigDecimal("6.0"), List.of("static", "lightning-rod"), "ピカチュウ", "Kanto",
            List.of("starter", "electric"));

    @Mock
    private LocalPokemonRepositoryPort localPokemonRepositoryPort;

    @InjectMocks
    private UpdateLocalPokemonService service;

    private static UpdateLocalPokemonCommand command(final String name, final String spriteUrl, final String category,
            final BigDecimal weightKg, final List<String> abilities, final String localizedName, final String region,
            final List<String> internalTags) {
        return new UpdateLocalPokemonCommand(USER_ID, 10L, name, spriteUrl, category, weightKg, abilities,
                localizedName, region, internalTags);
    }

    @Test
    void shouldReplaceEveryFieldExceptIdentifiersWhenRecordExists() {
        // given
        final var replaced = new LocalPokemon(10L, 25, "raichu", "https://img/26.png", "Mouse Pokémon",
                new BigDecimal("30.0"), List.of("static"), "ライチュウ", "Johto", List.of("mascot"));
        when(localPokemonRepositoryPort.findById(USER_ID, 10L)).thenReturn(Optional.of(STORED));
        when(localPokemonRepositoryPort.save(USER_ID, replaced)).thenReturn(replaced);

        // when
        final var result = service.update(command("raichu", "https://img/26.png", "Mouse Pokémon",
                new BigDecimal("30.0"), List.of("static"), "ライチュウ", "Johto", List.of("mascot")));

        // then
        assertThat(result).isEqualTo(new LocalPokemonResult(10L, 25, "raichu", "https://img/26.png", "Mouse Pokémon",
                new BigDecimal("30.0"), List.of("static"), "ライチュウ", "Johto", List.of("mascot")));
    }

    @Test
    void shouldReplaceOptionalFieldsWithNullAndEmptyTagsWhenTheyAreAbsent() {
        // given
        when(localPokemonRepositoryPort.findById(USER_ID, 10L)).thenReturn(Optional.of(STORED));
        when(localPokemonRepositoryPort.save(eq(USER_ID), any())).thenAnswer(invocation -> invocation.getArgument(1));

        // when
        final var result = service.update(command("pikachu", null, null, new BigDecimal("6.0"), List.of("static"),
                null, null, null));

        // then
        verify(localPokemonRepositoryPort).save(USER_ID, new LocalPokemon(10L, 25, "pikachu", null, null,
                new BigDecimal("6.0"), List.of("static"), null, null, List.of()));
        assertThat(result.spriteUrl()).isNull();
        assertThat(result.category()).isNull();
        assertThat(result.localizedName()).isNull();
        assertThat(result.region()).isNull();
        assertThat(result.internalTags()).isEmpty();
    }

    @Test
    void shouldThrowNotFoundWithoutSavingWhenUserHasNoCopyWithThatId() {
        // given
        when(localPokemonRepositoryPort.findById(USER_ID, 10L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.update(command("pikachu", null, null, new BigDecimal("6.0"),
                List.of("static"), null, null, null)))
                .isInstanceOf(LocalPokemonNotFoundException.class)
                .hasMessage("Local Pokemon not found: 10");

        verify(localPokemonRepositoryPort, never()).save(anyLong(), any());
    }

    @Test
    void shouldRejectUpdateWithoutSavingWhenAValueBreaksADomainInvariant() {
        // given
        when(localPokemonRepositoryPort.findById(USER_ID, 10L)).thenReturn(Optional.of(STORED));

        // when & then
        assertThatThrownBy(() -> service.update(command("pikachu", null, null, new BigDecimal("6.0"), List.of(),
                null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Pokemon must have 1 to 10 abilities");

        verify(localPokemonRepositoryPort, never()).save(anyLong(), any());
    }
}
