package com.tech.challenge.infrastructure.pokemon.pokeapi.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.tech.challenge.infrastructure.pokemon.pokeapi.PokeApiFixtures;

class PokeApiModelDeserializationTest {

    @Test
    void shouldReadOnlyDeclaredFieldsWhenDeserializingRecordedPokemon() {
        // when
        final var pokemon = PokeApiFixtures.read("pokemon/1.json", PokeApiPokemon.class);

        // then
        assertThat(pokemon.id()).isEqualTo(1);
        assertThat(pokemon.name()).isEqualTo("bulbasaur");
        assertThat(pokemon.sprites().frontDefault())
                .isEqualTo("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png");
        assertThat(pokemon.stats()).hasSize(6);
        assertThat(pokemon.stats().getFirst().stat().name()).isEqualTo("hp");
        assertThat(pokemon.stats().getFirst().baseStat()).isEqualTo(45);
        assertThat(pokemon.species().url()).isEqualTo("https://pokeapi.co/api/v2/pokemon-species/1/");
    }

    @Test
    void shouldReadFlavorTextsAndChainUrlWhenDeserializingRecordedSpecies() {
        // when
        final var species = PokeApiFixtures.read("pokemon-species/133.json", PokeApiSpecies.class);

        // then
        assertThat(species.flavorTextEntries()).isNotEmpty();
        assertThat(species.flavorTextEntries().getFirst().language().name()).isNotBlank();
        assertThat(species.evolutionChain().url()).isEqualTo("https://pokeapi.co/api/v2/evolution-chain/67/");
    }

    @Test
    void shouldReadRecursiveLinksWhenDeserializingRecordedEvolutionChain() {
        // when
        final var chain = PokeApiFixtures.read("evolution-chain/1.json", PokeApiEvolutionChain.class);

        // then
        assertThat(chain.chain().species().name()).isEqualTo("bulbasaur");
        assertThat(chain.chain().evolvesTo().getFirst().evolvesTo().getFirst().species().name())
                .isEqualTo("venusaur");
    }

    @Test
    void shouldReadWeightAndAbilitiesWhenDeserializingRecordedPokemon() {
        // when
        final var pokemon = PokeApiFixtures.read("pokemon/1.json", PokeApiPokemon.class);

        // then
        assertThat(pokemon.weight()).isEqualTo(69);
        assertThat(pokemon.abilities()).extracting(slot -> slot.ability().name())
                .containsExactly("overgrow", "chlorophyll");
    }

    @Test
    void shouldReadGeneraWhenDeserializingRecordedSpecies() {
        // when
        final var species = PokeApiFixtures.read("pokemon-species/1.json", PokeApiSpecies.class);

        // then
        assertThat(species.genera()).anySatisfy(genus -> {
            assertThat(genus.genus()).isEqualTo("Seed Pokémon");
            assertThat(genus.language().name()).isEqualTo("en");
        });
    }

    @Test
    void shouldReadCountAndResultsWhenDeserializingRecordedList() {
        // when
        final var list = PokeApiFixtures.read("pokemon-list/offset-0-limit-2.json", PokeApiPokemonList.class);

        // then
        assertThat(list.count()).isEqualTo(1351);
        assertThat(list.results()).extracting(PokeApiNamedResource::url).containsExactly(
                "https://pokeapi.co/api/v2/pokemon/1/",
                "https://pokeapi.co/api/v2/pokemon/2/");
    }
}
