package com.tech.challenge.web.pokemon.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
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

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LocalPokemonOwnershipIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final long ASH = 1101L;
    private static final long MISTY = 1102L;
    private static final String BASE = "/api/v1/local/pokemon";
    private static final String SNORLAX = "{\"pokemon\":\"snorlax\"}";

    private final MockMvc mockMvc;
    private final JwtEncoder jwtEncoder;
    private final JdbcTemplate jdbcTemplate;

    @MockitoBean
    private PokemonCatalogPort pokemonCatalogPort;

    @Autowired
    LocalPokemonOwnershipIntegrationTest(final MockMvc mockMvc, final JwtEncoder jwtEncoder,
            final JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.jwtEncoder = jwtEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void createUsersAndCatalog() {
        for (final long id : List.of(ASH, MISTY)) {
            jdbcTemplate.update("MERGE INTO users (id, name, email, password_hash) KEY (id) VALUES (?, ?, ?, ?)", id,
                    "user" + id, "user" + id + "@example.com", "hash");
        }
        when(pokemonCatalogPort.findSummary("snorlax")).thenReturn(Optional.of(new PokemonSummary(143, "snorlax",
                "https://img/143.png", "Sleeping Pokémon", new BigDecimal("460.0"), List.of("immunity"))));
    }

    @AfterEach
    void removeCopies() {
        final var copies = "(SELECT id FROM local_pokemon WHERE user_id IN (?, ?))";
        jdbcTemplate.update("DELETE FROM local_pokemon_abilities WHERE local_pokemon_id IN " + copies, ASH, MISTY);
        jdbcTemplate.update("DELETE FROM local_pokemon_internal_tags WHERE local_pokemon_id IN " + copies, ASH, MISTY);
        jdbcTemplate.update("DELETE FROM local_pokemon WHERE user_id IN (?, ?)", ASH, MISTY);
    }

    private String bearer(final long userId) {
        final var now = Instant.now();
        final var claims = JwtClaimsSet.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofHours(1)))
                .build();
        return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                claims)).getTokenValue();
    }

    private int syncStatus(final long userId) throws Exception {
        return mockMvc.perform(post(BASE).header(HttpHeaders.AUTHORIZATION, bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON).content(SNORLAX))
                .andReturn().getResponse().getStatus();
    }

    private long sync(final long userId) throws Exception {
        final var response = mockMvc.perform(post(BASE).header(HttpHeaders.AUTHORIZATION, bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON).content(SNORLAX))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(response).get("id").asLong();
    }

    private List<Long> listedIds(final long userId) throws Exception {
        final var response = mockMvc.perform(get(BASE + "?size=100").header(HttpHeaders.AUTHORIZATION, bearer(userId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(response).get("content").valueStream().map(pokemon -> pokemon.get("id").asLong())
                .toList();
    }

    @Test
    void shouldKeepEachUsersCopyApartWhenTwoUsersSyncTheSamePokemon() throws Exception {
        // given
        final var ashCopy = sync(ASH);

        // when
        final var mistyCopy = sync(MISTY);

        // then
        assertThat(mistyCopy).isNotEqualTo(ashCopy);
        assertThat(listedIds(ASH)).containsExactly(ashCopy);
        assertThat(listedIds(MISTY)).containsExactly(mistyCopy);
    }

    @Test
    void shouldAnswerConflictWhenTheSameUserSyncsAPokemonTwice() throws Exception {
        // given
        sync(ASH);

        // when & then
        assertThat(syncStatus(ASH)).isEqualTo(409);
    }

    @Test
    void shouldAnswerNotFoundWhenAUserReadsUpdatesOrDeletesAnotherUsersCopy() throws Exception {
        // given
        final var path = BASE + "/" + sync(ASH);
        final var update = "{\"name\":\"snorlax\",\"weightKg\":460.0,\"abilities\":[\"immunity\"]}";

        // when & then
        mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, bearer(MISTY))).andExpect(status().isNotFound());
        mockMvc.perform(put(path).header(HttpHeaders.AUTHORIZATION, bearer(MISTY))
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(path).header(HttpHeaders.AUTHORIZATION, bearer(MISTY)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, bearer(ASH))).andExpect(status().isOk());
    }
}
