package com.tech.challenge.infrastructure.pokemon.pokeapi.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.web.client.ResourceAccessException;

import com.tech.challenge.domain.pokemon.model.PokemonSummary;
import com.tech.challenge.domain.shared.exception.ExternalServiceUnavailableException;
import com.tech.challenge.infrastructure.pokemon.pokeapi.client.PokeApiClient;
import com.tech.challenge.infrastructure.pokemon.pokeapi.mapper.PokeApiDetailsMapper;
import com.tech.challenge.infrastructure.pokemon.pokeapi.model.PokeApiNamedResource;
import com.tech.challenge.infrastructure.pokemon.pokeapi.model.PokeApiPokemon;
import com.tech.challenge.infrastructure.pokemon.pokeapi.model.PokeApiPokemonList;
import com.tech.challenge.infrastructure.pokemon.pokeapi.model.PokeApiSpecies;

class PokeApiCatalogAdapterConcurrencyTest {

    private static final int ITEMS = 30;
    private static final int MAX_CONCURRENT_CALLS = 10;

    private final AtomicInteger inFlight = new AtomicInteger();
    private final AtomicInteger maxInFlight = new AtomicInteger();
    private final AtomicInteger pokemonCalls = new AtomicInteger();
    private final AtomicInteger uninterruptedCalls = new AtomicInteger();
    private final CountDownLatch neverReleased = new CountDownLatch(1);

    private final PokeApiClient pokeApiClient = mock(PokeApiClient.class);
    private final PokeApiCatalogAdapter adapter =
            new PokeApiCatalogAdapter(pokeApiClient, Mappers.getMapper(PokeApiDetailsMapper.class), MAX_CONCURRENT_CALLS);

    @BeforeEach
    void stubSlowPokeApi() {
        final var results = IntStream.rangeClosed(1, ITEMS)
                .mapToObj(id -> new PokeApiNamedResource("pokemon-" + id, "https://pokeapi.co/api/v2/pokemon/" + id + "/"))
                .toList();
        when(pokeApiClient.getPokemonList(anyLong(), anyInt())).thenReturn(new PokeApiPokemonList(ITEMS, results));
        when(pokeApiClient.getPokemon(anyString())).thenAnswer(invocation -> slowCall(() -> {
            final String url = invocation.getArgument(0);
            final var id = Integer.parseInt(url.replaceAll(".*/(\\d+)/$", "$1"));
            return new PokeApiPokemon(id, "pokemon-" + id, null, List.of(),
                    new PokeApiNamedResource("pokemon-" + id, "https://pokeapi.co/api/v2/pokemon-species/" + id + "/"),
                    10, List.of());
        }));
        when(pokeApiClient.getSpecies(anyString())).thenAnswer(invocation -> slowCall(() -> new PokeApiSpecies(null, null, null)));
    }

    private <T> T slowCall(final Supplier<T> response) throws InterruptedException {
        maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
        try {
            Thread.sleep(20);
            return response.get();
        } finally {
            inFlight.decrementAndGet();
        }
    }

    @Test
    void shouldFetchItemsInParallelKeepingListOrderWhenPageHasManyItems() {
        // when
        final var page = adapter.findSummaries(0, ITEMS);

        // then
        assertThat(page.content()).extracting(PokemonSummary::name)
                .containsExactlyElementsOf(IntStream.rangeClosed(1, ITEMS).mapToObj(id -> "pokemon-" + id).toList());
        assertThat(maxInFlight.get()).isGreaterThan(1);
    }

    @Test
    void shouldKeepAtMostTenPokeApiCallsInFlightWhenSeveralPagesAreRequestedAtOnce() {
        // when
        final var first = CompletableFuture.supplyAsync(() -> adapter.findSummaries(0, ITEMS));
        final var second = CompletableFuture.supplyAsync(() -> adapter.findSummaries(1, ITEMS));
        first.join();
        second.join();

        // then
        assertThat(maxInFlight.get()).isBetween(2, MAX_CONCURRENT_CALLS);
    }

    @Test
    void shouldInterruptRemainingItemsWhenOneItemFails() {
        // given
        doAnswer(invocation -> {
            if (pokemonCalls.getAndIncrement() == 0) {
                throw new ResourceAccessException("read timed out");
            }
            neverReleased.await(5, TimeUnit.SECONDS);
            uninterruptedCalls.incrementAndGet();
            return null;
        }).when(pokeApiClient).getPokemon(anyString());

        // when & then
        assertThatThrownBy(() -> adapter.findSummaries(0, ITEMS))
                .isInstanceOf(ExternalServiceUnavailableException.class)
                .hasCauseInstanceOf(ResourceAccessException.class);
        assertThat(uninterruptedCalls.get()).isZero();
    }

    @Test
    void shouldThrowTheItemFailureUnwrappedWhenItIsNotAPokeApiError() {
        // given
        doAnswer(invocation -> slowCall(() -> new PokeApiPokemon(0, "broken", null, List.of(),
                new PokeApiNamedResource("broken", "https://pokeapi.co/api/v2/pokemon-species/0/"), 10, List.of())))
                .when(pokeApiClient).getPokemon(anyString());

        // when & then
        assertThatThrownBy(() -> adapter.findSummaries(0, ITEMS)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowUnavailableAndKeepInterruptFlagWhenWaitingForAPermitIsInterrupted() {
        // given
        Thread.currentThread().interrupt();

        // when
        final var thrown = catchThrowable(() -> adapter.findSummaries(0, ITEMS));
        final var interrupted = Thread.interrupted();

        // then
        assertThat(thrown).isInstanceOf(ExternalServiceUnavailableException.class);
        assertThat(interrupted).isTrue();
    }

    @Test
    void shouldRejectAdapterWhenMaxConcurrentCallsIsNotPositive() {
        // given
        final var mapper = Mappers.getMapper(PokeApiDetailsMapper.class);

        // when & then
        assertThatThrownBy(() -> new PokeApiCatalogAdapter(pokeApiClient, mapper, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
