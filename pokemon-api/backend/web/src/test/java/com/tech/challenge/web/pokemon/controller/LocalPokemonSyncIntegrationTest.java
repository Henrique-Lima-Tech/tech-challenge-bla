package com.tech.challenge.web.pokemon.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tech.challenge.application.pokemon.port.out.PokemonCatalogPort;
import com.tech.challenge.domain.pokemon.model.PokemonSummary;

/**
 * Sync through the real chain (security, validation, service, JPA, Flyway on H2 in memory) with the PokéAPI
 * mocked. The database lives as long as the JVM, so each test uses its own Pokémon.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LocalPokemonSyncIntegrationTest {

    /** The owner of the synced copies (D-31); far above the ids the auth tests generate. */
    private static final long USER_ID = 1001L;

    private final MockMvc mockMvc;
    private final JwtEncoder jwtEncoder;
    private final JdbcTemplate jdbcTemplate;

    @MockitoBean
    private PokemonCatalogPort pokemonCatalogPort;

    @Autowired
    LocalPokemonSyncIntegrationTest(final MockMvc mockMvc, final JwtEncoder jwtEncoder, final JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.jwtEncoder = jwtEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void createUser() {
        jdbcTemplate.update("MERGE INTO users (id, name, email, password_hash) KEY (id) VALUES (?, ?, ?, ?)", USER_ID,
                "user", "user" + USER_ID + "@example.com", "hash");
    }

    private String token() {
        final var now = Instant.now();
        final var claims = JwtClaimsSet.builder()
                .subject(String.valueOf(USER_ID))
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofHours(1)))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private int sync(final String token, final String pokemon) throws Exception {
        return mockMvc.perform(post("/api/v1/local/pokemon").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pokemon\":\"%s\",\"internalTags\":[\"mythical\"]}".formatted(pokemon)))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void shouldAnswerOneCreatedAndOneConflictWhenTheSamePokemonIsSyncedConcurrently() throws Exception {
        // given
        final var mew = new PokemonSummary(151, "mew", "https://img/151.png", "New Species Pokémon",
                new BigDecimal("4.0"), List.of("synchronize"));
        // Both requests leave the PokéAPI step together, so neither can see the other's row before inserting.
        final var bothFetched = new CyclicBarrier(2);
        when(pokemonCatalogPort.findSummary("mew")).thenAnswer(invocation -> {
            bothFetched.await(5, TimeUnit.SECONDS);
            return Optional.of(mew);
        });
        final var token = token();

        // when
        final List<Integer> statuses;
        try (final var executor = Executors.newFixedThreadPool(2)) {
            final var first = executor.submit(() -> sync(token, "mew"));
            final var second = executor.submit(() -> sync(token, "Mew"));
            statuses = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        }

        // then
        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM local_pokemon WHERE poke_api_id = 151",
                Integer.class)).isEqualTo(1);
    }
}
