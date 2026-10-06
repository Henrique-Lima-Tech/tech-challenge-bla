package com.tech.challenge.web.pokemon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tech.challenge.application.pokemon.port.in.DeleteLocalPokemonUseCase;
import com.tech.challenge.application.pokemon.port.in.GetLocalPokemonUseCase;
import com.tech.challenge.application.pokemon.port.in.GetPokemonDetailsUseCase;
import com.tech.challenge.application.pokemon.port.in.ListLocalPokemonUseCase;
import com.tech.challenge.application.pokemon.port.in.ListPokemonUseCase;
import com.tech.challenge.application.pokemon.port.in.SyncPokemonUseCase;
import com.tech.challenge.application.pokemon.port.in.UpdateLocalPokemonUseCase;
import com.tech.challenge.application.pokemon.port.out.LocalPokemonRepositoryPort;
import com.tech.challenge.application.pokemon.port.out.PokemonCatalogPort;
import com.tech.challenge.application.pokemon.service.DeleteLocalPokemonService;
import com.tech.challenge.application.pokemon.service.GetLocalPokemonService;
import com.tech.challenge.application.pokemon.service.GetPokemonDetailsService;
import com.tech.challenge.application.pokemon.service.ListLocalPokemonService;
import com.tech.challenge.application.pokemon.service.ListPokemonService;
import com.tech.challenge.application.pokemon.service.SyncPokemonService;
import com.tech.challenge.application.pokemon.service.UpdateLocalPokemonService;

@Configuration
public class PokemonUseCaseConfig {

    @Bean
    GetPokemonDetailsUseCase getPokemonDetailsUseCase(final PokemonCatalogPort pokemonCatalogPort) {
        return new GetPokemonDetailsService(pokemonCatalogPort);
    }

    @Bean
    ListPokemonUseCase listPokemonUseCase(final PokemonCatalogPort pokemonCatalogPort) {
        return new ListPokemonService(pokemonCatalogPort);
    }

    @Bean
    SyncPokemonUseCase syncPokemonUseCase(final PokemonCatalogPort pokemonCatalogPort,
            final LocalPokemonRepositoryPort localPokemonRepositoryPort) {
        return new SyncPokemonService(pokemonCatalogPort, localPokemonRepositoryPort);
    }

    @Bean
    UpdateLocalPokemonUseCase updateLocalPokemonUseCase(final LocalPokemonRepositoryPort localPokemonRepositoryPort) {
        return new UpdateLocalPokemonService(localPokemonRepositoryPort);
    }

    @Bean
    ListLocalPokemonUseCase listLocalPokemonUseCase(final LocalPokemonRepositoryPort localPokemonRepositoryPort) {
        return new ListLocalPokemonService(localPokemonRepositoryPort);
    }

    @Bean
    GetLocalPokemonUseCase getLocalPokemonUseCase(final LocalPokemonRepositoryPort localPokemonRepositoryPort) {
        return new GetLocalPokemonService(localPokemonRepositoryPort);
    }

    @Bean
    DeleteLocalPokemonUseCase deleteLocalPokemonUseCase(final LocalPokemonRepositoryPort localPokemonRepositoryPort) {
        return new DeleteLocalPokemonService(localPokemonRepositoryPort);
    }
}
