package com.tech.challenge.infrastructure.pokemon.pokeapi.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.tech.challenge.domain.pokemon.model.EvolutionStage;
import com.tech.challenge.domain.pokemon.model.PokemonDetails;
import com.tech.challenge.domain.pokemon.model.PokemonStat;
import com.tech.challenge.domain.pokemon.model.PokemonSummary;
import com.tech.challenge.infrastructure.pokemon.pokeapi.PokeApiFixtures;
import com.tech.challenge.infrastructure.pokemon.pokeapi.model.PokeApiEvolutionChain;
import com.tech.challenge.infrastructure.pokemon.pokeapi.model.PokeApiNamedResource;
import com.tech.challenge.infrastructure.pokemon.pokeapi.model.PokeApiPokemon;
import com.tech.challenge.infrastructure.pokemon.pokeapi.model.PokeApiSpecies;

class PokeApiDetailsMapperTest {

    private static final String SPRITES = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/";

    private final PokeApiDetailsMapper mapper = Mappers.getMapper(PokeApiDetailsMapper.class);

    private PokemonDetails map(final String pokemonId, final String chainId) {
        return mapper.toDetails(
                PokeApiFixtures.read("pokemon/" + pokemonId + ".json", PokeApiPokemon.class),
                PokeApiFixtures.read("pokemon-species/" + pokemonId + ".json", PokeApiSpecies.class),
                PokeApiFixtures.read("evolution-chain/" + chainId + ".json", PokeApiEvolutionChain.class));
    }

    private PokemonSummary summary(final String pokemonId) {
        return mapper.toSummary(
                PokeApiFixtures.read("pokemon/" + pokemonId + ".json", PokeApiPokemon.class),
                PokeApiFixtures.read("pokemon-species/" + pokemonId + ".json", PokeApiSpecies.class));
    }

    @Test
    void shouldMapIdNameAndImageWhenPokemonIsRecorded() {
        // when
        final var details = map("1", "1");

        // then
        assertThat(details.id()).isEqualTo(1);
        assertThat(details.name()).isEqualTo("bulbasaur");
        assertThat(details.imageUrl())
                .isEqualTo("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png");
    }

    @Test
    void shouldMapEveryStatInPokeApiOrderWhenPokemonIsRecorded() {
        // when
        final var details = map("1", "1");

        // then
        assertThat(details.stats()).containsExactly(
                new PokemonStat("hp", 45),
                new PokemonStat("attack", 49),
                new PokemonStat("defense", 49),
                new PokemonStat("special-attack", 65),
                new PokemonStat("special-defense", 65),
                new PokemonStat("speed", 45));
    }

    @Test
    void shouldUseFirstEnglishFlavorTextOnOneLineWhenSpeciesIsRecorded() {
        // when
        final var details = map("1", "1");

        // then
        assertThat(details.description())
                .isEqualTo("A strange seed was planted on its back at birth. The plant sprouts and grows with this POKéMON.");
    }

    @Test
    void shouldJoinWordsSplitBySoftHyphenWhenCleaningFlavorText() {
        // given
        final var english = new PokeApiNamedResource("en", null);
        final var species = new PokeApiSpecies(
                List.of(new PokeApiSpecies.FlavorTextEntry("to alter the com­\nposition of its\fbody", english)),
                null, null);

        // when
        final var description = mapper.toDescription(species);

        // then
        assertThat(description).isEqualTo("to alter the composition of its body");
    }

    @Test
    void shouldReturnNullDescriptionWhenNoEnglishFlavorTextExists() {
        // given
        final var japanese = new PokeApiNamedResource("ja", null);
        final var onlyJapanese = new PokeApiSpecies(List.of(new PokeApiSpecies.FlavorTextEntry("たね", japanese)), null, null);
        final var noEntries = new PokeApiSpecies(null, null, null);

        // when & then
        assertThat(mapper.toDescription(onlyJapanese)).isNull();
        assertThat(mapper.toDescription(noEntries)).isNull();
    }

    @Test
    void shouldMapLinearChainWhenEvolutionChainHasNoBranches() {
        // when
        final var details = map("1", "1");

        // then
        assertThat(details.evolutionChain()).isEqualTo(
                new EvolutionStage("bulbasaur", SPRITES + "1.png", List.of(
                        new EvolutionStage("ivysaur", SPRITES + "2.png", List.of(
                                new EvolutionStage("venusaur", SPRITES + "3.png", List.of()))))));
    }

    @Test
    void shouldBuildSpriteUrlFromSpeciesIdWhenEvolutionChainBranches() {
        // when
        final var details = map("133", "67");

        // then
        assertThat(details.evolutionChain().spriteUrl()).isEqualTo(SPRITES + "133.png");
        assertThat(details.evolutionChain().evolvesTo())
                .extracting(EvolutionStage::spriteUrl)
                .containsExactly(SPRITES + "134.png", SPRITES + "135.png", SPRITES + "136.png", SPRITES + "196.png",
                        SPRITES + "197.png", SPRITES + "470.png", SPRITES + "471.png", SPRITES + "700.png");
    }

    @Test
    void shouldBuildSpriteUrlWhenSpeciesUrlHasNoTrailingSlash() {
        // when
        final var spriteUrl = mapper.toSpriteUrl("https://pokeapi.co/api/v2/pokemon-species/25");

        // then
        assertThat(spriteUrl).isEqualTo(SPRITES + "25.png");
    }

    @Test
    void shouldReturnNullSpriteUrlWhenSpeciesUrlHasNoNumericId() {
        // when & then
        assertThat(mapper.toSpriteUrl(null)).isNull();
        assertThat(mapper.toSpriteUrl("  ")).isNull();
        assertThat(mapper.toSpriteUrl("https://pokeapi.co/api/v2/pokemon-species/eevee/")).isNull();
    }

    @Test
    void shouldKeepEveryBranchWhenEvolutionChainBranches() {
        // when
        final var details = map("133", "67");

        // then
        assertThat(details.name()).isEqualTo("eevee");
        assertThat(details.evolutionChain().name()).isEqualTo("eevee");
        assertThat(details.evolutionChain().evolvesTo())
                .extracting(EvolutionStage::name)
                .containsExactly("vaporeon", "jolteon", "flareon", "espeon", "umbreon", "leafeon", "glaceon", "sylveon");
        assertThat(details.evolutionChain().evolvesTo()).allSatisfy(stage -> assertThat(stage.evolvesTo()).isEmpty());
    }

    @Test
    void shouldMapSummaryWhenPokemonAndSpeciesAreRecorded() {
        // when
        final var summary = summary("1");

        // then
        assertThat(summary.id()).isEqualTo(1);
        assertThat(summary.name()).isEqualTo("bulbasaur");
        assertThat(summary.spriteUrl())
                .isEqualTo("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png");
        assertThat(summary.category()).isEqualTo("Seed Pokémon");
        assertThat(summary.weightKg()).isEqualTo(new BigDecimal("6.9"));
    }

    @Test
    void shouldIncludeHiddenAbilitiesInPokeApiOrderWhenMappingSummary() {
        // when
        final var bulbasaur = summary("1");
        final var eevee = summary("133");

        // then
        assertThat(bulbasaur.abilities()).containsExactly("overgrow", "chlorophyll");
        assertThat(eevee.abilities()).containsExactly("run-away", "adaptability", "anticipation");
    }

    @Test
    void shouldKeepOneDecimalPlaceWhenWeightIsAWholeNumberOfKilograms() {
        // when
        final var summary = summary("2");

        // then
        assertThat(summary.weightKg()).isEqualTo(new BigDecimal("13.0"));
    }

    @Test
    void shouldReturnNullCategoryWhenNoEnglishGenusExists() {
        // given
        final var japanese = new PokeApiNamedResource("ja", null);
        final var onlyJapanese = new PokeApiSpecies(null, null, List.of(new PokeApiSpecies.Genus("たねポケモン", japanese)));
        final var noGenera = new PokeApiSpecies(null, null, null);

        // when & then
        assertThat(mapper.toCategory(onlyJapanese)).isNull();
        assertThat(mapper.toCategory(noGenera)).isNull();
    }
}
