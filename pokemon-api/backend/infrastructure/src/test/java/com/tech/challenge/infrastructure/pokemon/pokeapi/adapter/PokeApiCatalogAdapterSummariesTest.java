package com.tech.challenge.infrastructure.pokemon.pokeapi.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.between;
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
import org.springframework.boot.restclient.test.MockServerRestClientCustomizer;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.UnorderedRequestExpectationManager;

import com.tech.challenge.domain.pokemon.model.PokemonSummary;
import com.tech.challenge.domain.shared.exception.ExternalServiceUnavailableException;
import com.tech.challenge.infrastructure.pokemon.pokeapi.PokeApiFixtures;
import com.tech.challenge.infrastructure.pokemon.pokeapi.client.PokeApiClient;
import com.tech.challenge.infrastructure.pokemon.pokeapi.config.PokeApiConfig;
import com.tech.challenge.infrastructure.pokemon.pokeapi.mapper.PokeApiDetailsMapperImpl;

@RestClientTest(properties = { "pokeapi.base-url=https://pokeapi.co/api/v2", "pokeapi.max-concurrent-calls=10" })
@Import({ PokeApiConfig.class, PokeApiClient.class, PokeApiDetailsMapperImpl.class, PokeApiCatalogAdapter.class })
class PokeApiCatalogAdapterSummariesTest {

    private static final String BASE = "https://pokeapi.co/api/v2/";
    private static final String FIRST_PAGE = "pokemon?offset=0&limit=2";

    @TestConfiguration
    static class UnorderedServerConfig {

        @Bean
        MockServerRestClientCustomizer mockServerRestClientCustomizer() {
            return new MockServerRestClientCustomizer(UnorderedRequestExpectationManager::new);
        }
    }

    private final MockRestServiceServer server;
    private final PokeApiCatalogAdapter adapter;

    @Autowired
    PokeApiCatalogAdapterSummariesTest(final MockRestServiceServer server, final PokeApiCatalogAdapter adapter) {
        this.server = server;
        this.adapter = adapter;
    }

    private void expect(final String url, final String fixture) {
        server.expect(requestTo(BASE + url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(PokeApiFixtures.read(fixture), MediaType.APPLICATION_JSON));
    }

    private void allow(final String url, final String fixture) {
        server.expect(between(0, 1), requestTo(BASE + url))
                .andRespond(withSuccess(PokeApiFixtures.read(fixture), MediaType.APPLICATION_JSON));
    }

    private void expectEmptyList(final String url) {
        server.expect(requestTo(BASE + url))
                .andRespond(withSuccess("{\"count\": 1351, \"results\": []}", MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldReturnItemsInListOrderWhenListAndItemsAreFound() {
        // given
        expect(FIRST_PAGE, "pokemon-list/offset-0-limit-2.json");
        expect("pokemon/1/", "pokemon/1.json");
        expect("pokemon-species/1/", "pokemon-species/1.json");
        expect("pokemon/2/", "pokemon/2.json");
        expect("pokemon-species/2/", "pokemon-species/2.json");

        // when
        final var page = adapter.findSummaries(0, 2);

        // then
        assertThat(page.content()).extracting(PokemonSummary::name).containsExactly("bulbasaur", "ivysaur");
        assertThat(page.content().get(1).category()).isEqualTo("Seed Pokémon");
        assertThat(page.content().get(1).weightKg()).isEqualTo(new BigDecimal("13.0"));
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(1351);
        assertThat(page.totalPages()).isEqualTo(676);
        server.verify();
    }

    @Test
    void shouldReturnEmptyContentWithTotalWhenPageIsPastTheEnd() {
        // given
        expectEmptyList("pokemon?offset=2000&limit=2");

        // when
        final var page = adapter.findSummaries(1000, 2);

        // then
        assertThat(page.content()).isEmpty();
        assertThat(page.page()).isEqualTo(1000);
        assertThat(page.totalElements()).isEqualTo(1351);
        server.verify();
    }

    @Test
    void shouldComputeOffsetWithoutOverflowWhenPageIsVeryLarge() {
        // given
        expectEmptyList("pokemon?offset=214748364700&limit=100");

        // when
        final var page = adapter.findSummaries(Integer.MAX_VALUE, 100);

        // then
        assertThat(page.content()).isEmpty();
        server.verify();
    }

    @Test
    void shouldThrowUnavailableWhenPokeApiFailsOnList() {
        // given
        server.expect(requestTo(BASE + FIRST_PAGE)).andRespond(withServerError());

        // when & then
        assertThatThrownBy(() -> adapter.findSummaries(0, 2)).isInstanceOf(ExternalServiceUnavailableException.class);
    }

    @Test
    void shouldThrowUnavailableWhenPokeApiCannotBeReached() {
        // given
        server.expect(requestTo(BASE + FIRST_PAGE)).andRespond(withException(new IOException("connection refused")));

        // when & then
        assertThatThrownBy(() -> adapter.findSummaries(0, 2)).isInstanceOf(ExternalServiceUnavailableException.class);
    }

    @Test
    void shouldThrowUnavailableWhenPokeApiFailsOnAnItem() {
        // given
        expect(FIRST_PAGE, "pokemon-list/offset-0-limit-2.json");
        server.expect(requestTo(BASE + "pokemon/1/")).andRespond(withServerError());
        allow("pokemon/2/", "pokemon/2.json");
        allow("pokemon-species/2/", "pokemon-species/2.json");

        // when & then
        assertThatThrownBy(() -> adapter.findSummaries(0, 2)).isInstanceOf(ExternalServiceUnavailableException.class);
    }

    @Test
    void shouldThrowUnavailableWhenPokeApiFailsOnASpecies() {
        // given
        expect(FIRST_PAGE, "pokemon-list/offset-0-limit-2.json");
        allow("pokemon/1/", "pokemon/1.json");
        allow("pokemon-species/1/", "pokemon-species/1.json");
        expect("pokemon/2/", "pokemon/2.json");
        server.expect(requestTo(BASE + "pokemon-species/2/")).andRespond(withServerError());

        // when & then
        assertThatThrownBy(() -> adapter.findSummaries(0, 2)).isInstanceOf(ExternalServiceUnavailableException.class);
    }

    @Test
    void shouldThrowUnavailableWhenAnItemIsNotFound() {
        // given
        expect(FIRST_PAGE, "pokemon-list/offset-0-limit-2.json");
        allow("pokemon/1/", "pokemon/1.json");
        allow("pokemon-species/1/", "pokemon-species/1.json");
        server.expect(requestTo(BASE + "pokemon/2/")).andRespond(withResourceNotFound());

        // when & then
        assertThatThrownBy(() -> adapter.findSummaries(0, 2)).isInstanceOf(ExternalServiceUnavailableException.class);
    }
}
