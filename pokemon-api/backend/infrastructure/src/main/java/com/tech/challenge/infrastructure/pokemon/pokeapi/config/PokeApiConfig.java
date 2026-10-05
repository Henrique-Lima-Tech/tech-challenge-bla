package com.tech.challenge.infrastructure.pokemon.pokeapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

/**
 * PokéAPI {@link RestClient} (D-16). Timeouts come from {@code spring.http.clients.*}, applied by Spring Boot
 * to the auto-configured {@link RestClient.Builder}.
 */
@Slf4j
@Configuration
public class PokeApiConfig {

    @Bean
    RestClient pokeApiRestClient(final RestClient.Builder builder, @Value("${pokeapi.base-url}") final String baseUrl) {
        log.info("PokeAPI base URL: {}", baseUrl);
        return builder.baseUrl(baseUrl).build();
    }
}
