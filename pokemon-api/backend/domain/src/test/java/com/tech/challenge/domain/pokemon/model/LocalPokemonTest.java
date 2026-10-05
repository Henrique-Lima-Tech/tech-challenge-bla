package com.tech.challenge.domain.pokemon.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LocalPokemonTest {

    private static final BigDecimal WEIGHT = new BigDecimal("6.0");
    private static final List<String> ABILITIES = List.of("static", "lightning-rod");

    private static LocalPokemon withName(final String name) {
        return new LocalPokemon(null, 25, name, null, null, WEIGHT, ABILITIES, null, null, null);
    }

    private static LocalPokemon withSpriteUrl(final String spriteUrl) {
        return new LocalPokemon(null, 25, "pikachu", spriteUrl, null, WEIGHT, ABILITIES, null, null, null);
    }

    private static LocalPokemon withWeight(final BigDecimal weightKg) {
        return new LocalPokemon(null, 25, "pikachu", null, null, weightKg, ABILITIES, null, null, null);
    }

    private static LocalPokemon withAbilities(final List<String> abilities) {
        return new LocalPokemon(null, 25, "pikachu", null, null, WEIGHT, abilities, null, null, null);
    }

    private static LocalPokemon withTags(final List<String> internalTags) {
        return new LocalPokemon(null, 25, "pikachu", null, null, WEIGHT, ABILITIES, null, null, internalTags);
    }

    @Test
    void shouldCreateLocalPokemonWhenDataIsValid() {
        // when
        final var pokemon = new LocalPokemon(10L, 25, "pikachu", "https://img/25.png", "Mouse Pokémon", WEIGHT,
                ABILITIES, "ピカチュウ", "Kanto", List.of("starter", "electric"));

        // then
        assertThat(pokemon.id()).isEqualTo(10L);
        assertThat(pokemon.pokeApiId()).isEqualTo(25);
        assertThat(pokemon.name()).isEqualTo("pikachu");
        assertThat(pokemon.spriteUrl()).isEqualTo("https://img/25.png");
        assertThat(pokemon.category()).isEqualTo("Mouse Pokémon");
        assertThat(pokemon.weightKg()).isEqualTo(WEIGHT);
        assertThat(pokemon.abilities()).containsExactly("static", "lightning-rod");
        assertThat(pokemon.localizedName()).isEqualTo("ピカチュウ");
        assertThat(pokemon.region()).isEqualTo("Kanto");
        assertThat(pokemon.internalTags()).containsExactly("starter", "electric");
    }

    @Test
    void shouldAcceptNullIdAndOptionalFieldsWhenNotSavedYet() {
        // when
        final var pokemon = new LocalPokemon(null, 25, "pikachu", null, null, WEIGHT, ABILITIES, null, null, null);

        // then
        assertThat(pokemon.id()).isNull();
        assertThat(pokemon.spriteUrl()).isNull();
        assertThat(pokemon.category()).isNull();
        assertThat(pokemon.localizedName()).isNull();
        assertThat(pokemon.region()).isNull();
        assertThat(pokemon.internalTags()).isEmpty();
    }

    @Test
    void shouldRejectLocalPokemonWhenIdIsNotPositive() {
        // when & then
        assertThatThrownBy(() -> new LocalPokemon(0L, 25, "pikachu", null, null, WEIGHT, ABILITIES, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectLocalPokemonWhenPokeApiIdIsNotPositive() {
        // when & then
        assertThatThrownBy(() -> new LocalPokemon(null, 0, "pikachu", null, null, WEIGHT, ABILITIES, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAcceptNameWhenItHasFiftyCharacters() {
        // when & then
        assertThat(withName("a".repeat(50)).name()).hasSize(50);
    }

    @Test
    void shouldRejectLocalPokemonWhenNameIsBlankOrTooLong() {
        // when & then
        assertThatThrownBy(() -> withName(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withName(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withName("a".repeat(51))).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "http://img/25.png", "https://raw.githubusercontent.com/PokeAPI/sprites/master/25.png" })
    void shouldAcceptSpriteUrlWhenItIsAnHttpOrHttpsUrl(final String spriteUrl) {
        // when & then
        assertThat(withSpriteUrl(spriteUrl).spriteUrl()).isEqualTo(spriteUrl);
    }

    @Test
    void shouldAcceptSpriteUrlWhenItHasFiveHundredCharacters() {
        // given
        final var spriteUrl = "https://img/" + "a".repeat(488);

        // when & then
        assertThat(withSpriteUrl(spriteUrl).spriteUrl()).hasSize(500);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "img/25.png", "ftp://img/25.png", "javascript:alert(1)", "https://", "https://img/a b.png" })
    void shouldRejectLocalPokemonWhenSpriteUrlIsNotAnHttpUrl(final String spriteUrl) {
        // when & then
        assertThatThrownBy(() -> withSpriteUrl(spriteUrl)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectLocalPokemonWhenSpriteUrlIsTooLong() {
        // given
        final var spriteUrl = "https://img/" + "a".repeat(489);

        // when & then
        assertThatThrownBy(() -> withSpriteUrl(spriteUrl)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAcceptCategoryWhenItHasFiftyCharacters() {
        // when & then
        assertThat(new LocalPokemon(null, 25, "pikachu", null, "a".repeat(50), WEIGHT, ABILITIES, null, null, null)
                .category()).hasSize(50);
    }

    @Test
    void shouldRejectLocalPokemonWhenCategoryIsTooLong() {
        // when & then
        assertThatThrownBy(() -> new LocalPokemon(null, 25, "pikachu", null, "a".repeat(51), WEIGHT, ABILITIES, null,
                null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "0", "0.0", "6.9", "9999.9", "9999.90", "100" })
    void shouldAcceptWeightWhenWithinRangeAndOneDecimalPlace(final String weight) {
        // when & then
        assertThat(withWeight(new BigDecimal(weight)).weightKg()).isEqualByComparingTo(weight);
    }

    @ParameterizedTest
    @ValueSource(strings = { "-0.1", "10000", "10000.0", "6.95", "0.01" })
    void shouldRejectLocalPokemonWhenWeightIsOutOfRangeOrHasMoreThanOneDecimalPlace(final String weight) {
        // when & then
        assertThatThrownBy(() -> withWeight(new BigDecimal(weight))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectLocalPokemonWhenWeightIsNull() {
        // when & then
        assertThatThrownBy(() -> withWeight(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAcceptAbilitiesWhenThereAreOneToTenOfAtMostFiftyCharacters() {
        // given
        final var ten = List.of("a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "a9", "a".repeat(50));

        // when & then
        assertThat(withAbilities(List.of("static")).abilities()).containsExactly("static");
        assertThat(withAbilities(ten).abilities()).hasSize(10);
    }

    @Test
    void shouldRejectLocalPokemonWhenAbilitiesAreEmptyOrMoreThanTen() {
        // given
        final var eleven = List.of("a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "a9", "a10", "a11");

        // when & then
        assertThatThrownBy(() -> withAbilities(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withAbilities(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withAbilities(eleven)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectLocalPokemonWhenAnAbilityIsBlankOrTooLong() {
        // when & then
        assertThatThrownBy(() -> withAbilities(Collections.singletonList(null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withAbilities(List.of(" "))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withAbilities(List.of("a".repeat(51)))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectLocalPokemonWhenAbilitiesHaveDuplicates() {
        // when & then
        assertThatThrownBy(() -> withAbilities(List.of("static", "static")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAcceptLocalizedNameAndRegionWhenTheyHaveOneHundredCharacters() {
        // when
        final var pokemon = new LocalPokemon(null, 25, "pikachu", null, null, WEIGHT, ABILITIES, "a".repeat(100),
                "b".repeat(100), null);

        // then
        assertThat(pokemon.localizedName()).hasSize(100);
        assertThat(pokemon.region()).hasSize(100);
    }

    @Test
    void shouldRejectLocalPokemonWhenLocalizedNameOrRegionIsTooLong() {
        // when & then
        assertThatThrownBy(() -> new LocalPokemon(null, 25, "pikachu", null, null, WEIGHT, ABILITIES, "a".repeat(101),
                null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LocalPokemon(null, 25, "pikachu", null, null, WEIGHT, ABILITIES, null,
                "b".repeat(101), null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAcceptTagsWhenThereAreTwentyOfAtMostThirtyCharacters() {
        // given
        final var tags = new ArrayList<String>();
        for (var i = 1; i < 20; i++) {
            tags.add("tag" + i);
        }
        tags.add("t".repeat(30));

        // when & then
        assertThat(withTags(tags).internalTags()).hasSize(20);
    }

    @Test
    void shouldRejectLocalPokemonWhenThereAreMoreThanTwentyTags() {
        // given
        final var tags = new ArrayList<String>();
        for (var i = 0; i < 21; i++) {
            tags.add("tag" + i);
        }

        // when & then
        assertThatThrownBy(() -> withTags(tags)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectLocalPokemonWhenATagIsBlankOrTooLong() {
        // when & then
        assertThatThrownBy(() -> withTags(Collections.singletonList(null))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withTags(List.of(" "))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withTags(List.of("t".repeat(31)))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectLocalPokemonWhenTagsDifferOnlyInCase() {
        // when & then
        assertThatThrownBy(() -> withTags(List.of("Starter", "starter"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldKeepImmutableCopiesOfListsWhenSourceListsChange() {
        // given
        final var abilities = new ArrayList<>(List.of("static"));
        final var tags = new ArrayList<>(List.of("starter"));

        // when
        final var pokemon = new LocalPokemon(null, 25, "pikachu", null, null, WEIGHT, abilities, null, null, tags);
        abilities.add("lightning-rod");
        tags.add("electric");

        // then
        assertThat(pokemon.abilities()).containsExactly("static");
        assertThat(pokemon.internalTags()).containsExactly("starter");
        assertThatThrownBy(() -> pokemon.abilities().add("x")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> pokemon.internalTags().add("x")).isInstanceOf(UnsupportedOperationException.class);
    }
}
