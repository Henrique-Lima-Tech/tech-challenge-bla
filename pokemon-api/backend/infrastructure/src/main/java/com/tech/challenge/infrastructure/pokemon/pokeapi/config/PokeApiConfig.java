package com.tech.challenge.infrastructure.pokemon.pokeapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class PokeApiConfig {

    @Bean
    RestClient pokeApiRestClient(final RestClient.Builder builder, @Value("${pokeapi.base-url}") final String baseUrl) {
        log.info("PokeAPI base URL: {}", baseUrl);
        return builder.baseUrl(baseUrl).build();
    }
}
