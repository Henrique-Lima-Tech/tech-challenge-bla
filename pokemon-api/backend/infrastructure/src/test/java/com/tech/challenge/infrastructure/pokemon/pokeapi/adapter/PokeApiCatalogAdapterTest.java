package com.tech.challenge.infrastructure.pokemon.pokeapi.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import com.tech.challenge.domain.pokemon.model.EvolutionStage;
import com.tech.challenge.domain.shared.exception.ExternalServiceUnavailableException;
import com.tech.challenge.infrastructure.pokemon.pokeapi.PokeApiFixtures;
import com.tech.challenge.infrastructure.pokemon.pokeapi.client.PokeApiClient;
import com.tech.challenge.infrastructure.pokemon.pokeapi.config.PokeApiConfig;
import com.tech.challenge.infrastructure.pokemon.pokeapi.mapper.PokeApiDetailsMapperImpl;

@RestClientTest(properties = { "pokeapi.base-url=https://pokeapi.co/api/v2", "pokeapi.max-concurrent-calls=10" })
@Import({ PokeApiConfig.class, PokeApiClient.class, PokeApiDetailsMapperImpl.class, PokeApiCatalogAdapter.class })
class PokeApiCatalogAdapterTest {

    private static final String BASE = "https://pokeapi.co/api/v2/";

    private final MockRestServiceServer server;
    private final PokeApiCatalogAdapter adapter;

    @Autowired
    PokeApiCatalogAdapterTest(final MockRestServiceServer server, final PokeApiCatalogAdapter adapter) {
        this.server = server;
        this.adapter = adapter;
    }

    private void expect(final String url, final String fixture) {
        server.expect(requestTo(BASE + url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(PokeApiFixtures.read(fixture), MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldReturnDetailsWhenPokemonSpeciesAndChainAreFound() {
        // given
        expect("pokemon/bulbasaur/", "pokemon/1.json");
        expect("pokemon-species/1/", "pokemon-species/1.json");
        expect("evolution-chain/1/", "evolution-chain/1.json");

        // when
        final var details = adapter.findDetails("bulbasaur").orElseThrow();

        // then
        assertThat(details.id()).isEqualTo(1);
        assertThat(details.name()).isEqualTo("bulbasaur");
        assertThat(details.stats()).hasSize(6);
        assertThat(details.description()).startsWith("A strange seed was planted on its back at birth.");
        assertThat(details.evolutionChain().evolvesTo().getFirst().name()).isEqualTo("ivysaur");
        server.verify();
    }

    @Test
    void shouldFollowChainUrlFromSpeciesWhenChainIdDiffersFromPokemonId() {
        // given
        expect("pokemon/133/", "pokemon/133.json");
        expect("pokemon-species/133/", "pokemon-species/133.json");
        expect("evolution-chain/67/", "evolution-chain/67.json");

        // when
        final var details = adapter.findDetails("133").orElseThrow();

        // then
        assertThat(details.evolutionChain().evolvesTo()).extracting(EvolutionStage::name).hasSize(8);
        server.verify();
    }

    @Test
    void shouldReturnEmptyWhenPokeApiDoesNotKnowThePokemon() {
        // given
        server.expect(requestTo(BASE + "pokemon/missingno/")).andRespond(withResourceNotFound());

        // when
        final var details = adapter.findDetails("missingno");

        // then
        assertThat(details).isEmpty();
        server.verify();
    }

    @Test
    void shouldThrowUnavailableWhenPokeApiFailsOnPokemon() {
        // given
        server.expect(requestTo(BASE + "pokemon/1/")).andRespond(withServerError());

        // when & then
        assertThatThrownBy(() -> adapter.findDetails("1")).isInstanceOf(ExternalServiceUnavailableException.class);
    }

    @Test
    void shouldThrowUnavailableWhenPokeApiCannotBeReached() {
        // given
        server.expect(requestTo(BASE + "pokemon/1/")).andRespond(withException(new IOException("connection refused")));

        // when & then
        assertThatThrownBy(() -> adapter.findDetails("1")).isInstanceOf(ExternalServiceUnavailableException.class);
    }

    @Test
    void shouldThrowUnavailableWhenPokeApiFailsOnSpecies() {
        // given
        expect("pokemon/1/", "pokemon/1.json");
        server.expect(requestTo(BASE + "pokemon-species/1/")).andRespond(withServerError());

        // when & then
        assertThatThrownBy(() -> adapter.findDetails("1")).isInstanceOf(ExternalServiceUnavailableException.class);
    }

    @Test
    void shouldThrowUnavailableWhenEvolutionChainIsNotFound() {
        // given
        expect("pokemon/1/", "pokemon/1.json");
        expect("pokemon-species/1/", "pokemon-species/1.json");
        server.expect(requestTo(BASE + "evolution-chain/1/")).andRespond(withResourceNotFound());

        // when & then
        assertThatThrownBy(() -> adapter.findDetails("1")).isInstanceOf(ExternalServiceUnavailableException.class);
    }
}
