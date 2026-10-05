package com.tech.challenge.infrastructure.pokemon.pokeapi.adapter;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import com.tech.challenge.application.pokemon.port.out.PokemonCatalogPort;
import com.tech.challenge.application.shared.pagination.PageResult;
import com.tech.challenge.domain.pokemon.model.PokemonDetails;
import com.tech.challenge.domain.pokemon.model.PokemonSummary;
import com.tech.challenge.domain.shared.exception.ExternalServiceUnavailableException;
import com.tech.challenge.infrastructure.pokemon.pokeapi.client.PokeApiClient;
import com.tech.challenge.infrastructure.pokemon.pokeapi.mapper.PokeApiDetailsMapper;
import com.tech.challenge.infrastructure.pokemon.pokeapi.model.PokeApiNamedResource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PokeApiCatalogAdapter implements PokemonCatalogPort {

    private final PokeApiClient pokeApiClient;
    private final PokeApiDetailsMapper pokeApiDetailsMapper;
    /** D-28: shared by every request, so the PokéAPI never sees more than this many calls from us at once. */
    private final Semaphore pokeApiPermits;

    public PokeApiCatalogAdapter(final PokeApiClient pokeApiClient, final PokeApiDetailsMapper pokeApiDetailsMapper,
            @Value("${pokeapi.max-concurrent-calls}") final int maxConcurrentCalls) {
        if (maxConcurrentCalls < 1) {
            throw new IllegalArgumentException("pokeapi.max-concurrent-calls must be positive");
        }
        log.info("PokeAPI max concurrent calls: {}", maxConcurrentCalls);
        this.pokeApiClient = pokeApiClient;
        this.pokeApiDetailsMapper = pokeApiDetailsMapper;
        this.pokeApiPermits = new Semaphore(maxConcurrentCalls);
    }

    @Override
    public Optional<PokemonDetails> findDetails(final String idOrName) {
        try {
            return limited(() -> pokeApiClient.findPokemon(idOrName)).map(pokemon -> {
                final var species = limited(() -> pokeApiClient.getSpecies(pokemon.species().url()));
                final var chain = limited(() -> pokeApiClient.getEvolutionChain(species.evolutionChain().url()));
                final var details = pokeApiDetailsMapper.toDetails(pokemon, species, chain);
                log.debug("Built details for Pokemon '{}' (id {})", details.name(), details.id());
                return details;
            });
        } catch (final RestClientException e) {
            log.warn("PokeAPI request failed for Pokemon '{}'", idOrName, e);
            throw new ExternalServiceUnavailableException("PokeAPI is unavailable", e);
        }
    }

    @Override
    public Optional<PokemonSummary> findSummary(final String idOrName) {
        try {
            return limited(() -> pokeApiClient.findPokemon(idOrName)).map(pokemon -> {
                final var species = limited(() -> pokeApiClient.getSpecies(pokemon.species().url()));
                final var summary = pokeApiDetailsMapper.toSummary(pokemon, species);
                log.debug("Built summary for Pokemon '{}' (id {})", summary.name(), summary.id());
                return summary;
            });
        } catch (final RestClientException e) {
            log.warn("PokeAPI request failed for Pokemon '{}'", idOrName, e);
            throw new ExternalServiceUnavailableException("PokeAPI is unavailable", e);
        }
    }

    @Override
    public PageResult<PokemonSummary> findSummaries(final int page, final int size) {
        try {
            final var list = limited(() -> pokeApiClient.getPokemonList((long) page * size, size));
            final var summaries = fetchInParallel(list.results());
            log.debug("Built Pokemon page {} (size {}): {} items of {}", page, size, summaries.size(), list.count());
            return PageResult.of(summaries, page, size, list.count());
        } catch (final RestClientException e) {
            throw unavailable(page, size, e);
        } catch (final CompletionException e) {
            if (e.getCause() instanceof final RestClientException cause) {
                throw unavailable(page, size, cause);
            }
            if (e.getCause() instanceof final RuntimeException cause) {
                throw cause;
            }
            throw e;
        }
    }

    /**
     * D-19: one virtual thread per item; the result keeps the order of the PokéAPI list. The first failure
     * interrupts the other items, so a failing page does not hold D-28 permits until every item times out.
     */
    private List<PokemonSummary> fetchInParallel(final List<PokeApiNamedResource> results) {
        try (final var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            final var futures = results.stream()
                    .map(result -> CompletableFuture.supplyAsync(() -> fetchSummary(result.url()), executor))
                    .toList();
            final var firstFailure = new CompletableFuture<Void>();
            futures.forEach(future -> future.exceptionally(e -> {
                firstFailure.completeExceptionally(e);
                return null;
            }));
            try {
                CompletableFuture.anyOf(CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)), firstFailure)
                        .join();
            } catch (final CompletionException e) {
                executor.shutdownNow();
                throw e;
            }
            return futures.stream().map(CompletableFuture::join).toList();
        }
    }

    private PokemonSummary fetchSummary(final String url) {
        final var pokemon = limited(() -> pokeApiClient.getPokemon(url));
        final var species = limited(() -> pokeApiClient.getSpecies(pokemon.species().url()));
        return pokeApiDetailsMapper.toSummary(pokemon, species);
    }

    /** Holds a permit for one call only, never across two, so concurrent pages cannot deadlock each other. */
    private <T> T limited(final Supplier<T> call) {
        try {
            pokeApiPermits.acquire();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceUnavailableException("PokeAPI is unavailable", e);
        }
        try {
            return call.get();
        } finally {
            pokeApiPermits.release();
        }
    }

    private ExternalServiceUnavailableException unavailable(final int page, final int size, final RestClientException e) {
        log.warn("PokeAPI request failed for Pokemon page {} (size {})", page, size, e);
        return new ExternalServiceUnavailableException("PokeAPI is unavailable", e);
    }
}
