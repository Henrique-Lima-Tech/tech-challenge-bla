package com.tech.challenge.infrastructure.pokemon.pokeapi.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.restclient.test.MockServerRestClientCustomizer;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.UnorderedRequestExpectationManager;

import com.tech.challenge.infrastructure.pokemon.pokeapi.PokeApiFixtures;
import com.tech.challenge.infrastructure.pokemon.pokeapi.adapter.PokeApiCatalogAdapter;
import com.tech.challenge.infrastructure.pokemon.pokeapi.config.PokeApiConfig;
import com.tech.challenge.infrastructure.pokemon.pokeapi.mapper.PokeApiDetailsMapperImpl;
import com.tech.challenge.infrastructure.shared.cache.CacheConfig;

@RestClientTest(properties = { "pokeapi.base-url=https://pokeapi.co/api/v2", "pokeapi.max-concurrent-calls=10",
        "spring.cache.type=caffeine" })
@ImportAutoConfiguration(CacheAutoConfiguration.class)
@Import({ CacheConfig.class, PokeApiConfig.class, PokeApiClient.class, PokeApiDetailsMapperImpl.class,
        PokeApiCatalogAdapter.class })
class PokeApiClientCacheTest {

    private static final String BASE = "https://pokeapi.co/api/v2/";

    @TestConfiguration
    static class UnorderedServerConfig {

        @Bean
        MockServerRestClientCustomizer mockServerRestClientCustomizer() {
            return new MockServerRestClientCustomizer(UnorderedRequestExpectationManager::new);
        }
    }

    private final MockRestServiceServer server;
    private final PokeApiClient client;
    private final PokeApiCatalogAdapter adapter;
    private final CacheManager cacheManager;

    @Autowired
    PokeApiClientCacheTest(final MockRestServiceServer server, final PokeApiClient client,
            final PokeApiCatalogAdapter adapter, final CacheManager cacheManager) {
        this.server = server;
        this.client = client;
        this.adapter = adapter;
        this.cacheManager = cacheManager;
    }

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    private void expectOnce(final String url, final String fixture) {
        server.expect(requestTo(BASE + url))
                .andRespond(withSuccess(PokeApiFixtures.read(fixture), MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldCallPokeApiOnceWhenSameListIsRequestedTwice() {
        // given
        expectOnce("pokemon?offset=0&limit=2", "pokemon-list/offset-0-limit-2.json");

        // when
        final var first = client.getPokemonList(0, 2);
        final var second = client.getPokemonList(0, 2);

        // then
        assertThat(second).isEqualTo(first);
        server.verify();
    }

    @Test
    void shouldCallPokeApiAgainWhenListArgumentsDiffer() {
        // given
        expectOnce("pokemon?offset=0&limit=2", "pokemon-list/offset-0-limit-2.json");
        expectOnce("pokemon?offset=2&limit=2", "pokemon-list/offset-0-limit-2.json");

        // when
        client.getPokemonList(0, 2);
        client.getPokemonList(2, 2);

        // then
        server.verify();
    }

    @Test
    void shouldCallPokeApiOnceWhenSamePokemonUrlIsRequestedTwice() {
        // given
        expectOnce("pokemon/1/", "pokemon/1.json");

        // when
        client.getPokemon(BASE + "pokemon/1/");
        final var second = client.getPokemon(BASE + "pokemon/1/");

        // then
        assertThat(second.name()).isEqualTo("bulbasaur");
        server.verify();
    }

    @Test
    void shouldCallPokeApiOnceWhenSamePokemonIdOrNameIsRequestedTwice() {
        // given
        expectOnce("pokemon/bulbasaur/", "pokemon/1.json");

        // when
        client.findPokemon("bulbasaur");
        final var second = client.findPokemon("bulbasaur");

        // then
        assertThat(second).hasValueSatisfying(pokemon -> assertThat(pokemon.id()).isEqualTo(1));
        server.verify();
    }

    @Test
    void shouldCallPokeApiOnceWhenUnknownPokemonIsRequestedTwice() {
        // given
        server.expect(requestTo(BASE + "pokemon/missingno/")).andRespond(withResourceNotFound());

        // when
        client.findPokemon("missingno");
        final var second = client.findPokemon("missingno");

        // then
        assertThat(second).isEmpty();
        server.verify();
    }

    @Test
    void shouldCallPokeApiOnceWhenSameSpeciesIsRequestedTwice() {
        // given
        expectOnce("pokemon-species/1/", "pokemon-species/1.json");

        // when
        client.getSpecies(BASE + "pokemon-species/1/");
        client.getSpecies(BASE + "pokemon-species/1/");

        // then
        server.verify();
    }

    @Test
    void shouldCallPokeApiOnceWhenSameEvolutionChainIsRequestedTwice() {
        // given
        expectOnce("evolution-chain/1/", "evolution-chain/1.json");

        // when
        client.getEvolutionChain(BASE + "evolution-chain/1/");
        client.getEvolutionChain(BASE + "evolution-chain/1/");

        // then
        server.verify();
    }

    @Test
    void shouldNotCallPokeApiAgainWhenSamePageIsRequestedTwice() {
        // given
        expectOnce("pokemon?offset=0&limit=2", "pokemon-list/offset-0-limit-2.json");
        expectOnce("pokemon/1/", "pokemon/1.json");
        expectOnce("pokemon-species/1/", "pokemon-species/1.json");
        expectOnce("pokemon/2/", "pokemon/2.json");
        expectOnce("pokemon-species/2/", "pokemon-species/2.json");

        // when
        final var first = adapter.findSummaries(0, 2);
        final var second = adapter.findSummaries(0, 2);

        // then
        assertThat(second).isEqualTo(first);
        server.verify();
    }
}
