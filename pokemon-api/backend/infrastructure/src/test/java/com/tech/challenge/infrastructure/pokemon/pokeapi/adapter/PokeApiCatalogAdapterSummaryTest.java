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
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import com.tech.challenge.domain.shared.exception.ExternalServiceUnavailableException;
import com.tech.challenge.infrastructure.pokemon.pokeapi.PokeApiFixtures;
import com.tech.challenge.infrastructure.pokemon.pokeapi.client.PokeApiClient;
import com.tech.challenge.infrastructure.pokemon.pokeapi.config.PokeApiConfig;
import com.tech.challenge.infrastructure.pokemon.pokeapi.mapper.PokeApiDetailsMapperImpl;

@RestClientTest(properties = { "pokeapi.base-url=https://pokeapi.co/api/v2", "pokeapi.max-concurrent-calls=10" })
@Import({ PokeApiConfig.class, PokeApiClient.class, PokeApiDetailsMapperImpl.class, PokeApiCatalogAdapter.class })
class PokeApiCatalogAdapterSummaryTest {

    private static final String BASE = "https://pokeapi.co/api/v2/";

    private final MockRestServiceServer server;
    private final PokeApiCatalogAdapter adapter;

    @Autowired
    PokeApiCatalogAdapterSummaryTest(final MockRestServiceServer server, final PokeApiCatalogAdapter adapter) {
        this.server = server;
        this.adapter = adapter;
    }

    private void expect(final String url, final String fixture) {
        server.expect(requestTo(BASE + url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(PokeApiFixtures.read(fixture), MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldReturnSummaryWhenPokemonAndSpeciesAreFound() {
        // given
        expect("pokemon/bulbasaur/", "pokemon/1.json");
        expect("pokemon-species/1/", "pokemon-species/1.json");

        // when
        final var summary = adapter.findSummary("bulbasaur").orElseThrow();

        // then
        assertThat(summary.id()).isEqualTo(1);
        assertThat(summary.name()).isEqualTo("bulbasaur");
        assertThat(summary.spriteUrl()).startsWith("https://raw.githubusercontent.com/PokeAPI/sprites/").endsWith("/1.png");
        assertThat(summary.category()).isEqualTo("Seed Pokémon");
        assertThat(summary.weightKg()).isEqualTo(new BigDecimal("6.9"));
        assertThat(summary.abilities()).containsExactly("overgrow", "chlorophyll");
        server.verify();
    }

    @Test
    void shouldEncodeIdOrNameWhenItContainsAPathSeparator() {
        // given
        server.expect(requestTo(BASE + "pokemon/..%2Fberry%2F1/")).andRespond(withResourceNotFound());

        // when
        final var summary = adapter.findSummary("../berry/1");

        // then
        assertThat(summary).isEmpty();
        server.verify();
    }

    @Test
    void shouldReturnEmptyWhenPokeApiDoesNotKnowThePokemon() {
        // given
        server.expect(requestTo(BASE + "pokemon/missingno/")).andRespond(withResourceNotFound());

        // when
        final var summary = adapter.findSummary("missingno");

        // then
        assertThat(summary).isEmpty();
        server.verify();
    }

    @Test
    void shouldThrowUnavailableWhenPokeApiFailsOnPokemon() {
        // given
        server.expect(requestTo(BASE + "pokemon/1/")).andRespond(withServerError());

        // when & then
        assertThatThrownBy(() -> adapter.findSummary("1")).isInstanceOf(ExternalServiceUnavailableException.class);
    }

    @Test
    void shouldThrowUnavailableWhenPokeApiCannotBeReached() {
        // given
        server.expect(requestTo(BASE + "pokemon/1/")).andRespond(withException(new IOException("connection refused")));

        // when & then
        assertThatThrownBy(() -> adapter.findSummary("1")).isInstanceOf(ExternalServiceUnavailableException.class);
    }

    @Test
    void shouldThrowUnavailableWhenSpeciesIsNotFound() {
        // given
        expect("pokemon/1/", "pokemon/1.json");
        server.expect(requestTo(BASE + "pokemon-species/1/")).andRespond(withResourceNotFound());

        // when & then
        assertThatThrownBy(() -> adapter.findSummary("1")).isInstanceOf(ExternalServiceUnavailableException.class);
    }
}
