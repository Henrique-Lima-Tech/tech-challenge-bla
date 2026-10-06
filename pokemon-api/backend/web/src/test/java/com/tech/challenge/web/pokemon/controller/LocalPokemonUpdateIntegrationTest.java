package com.tech.challenge.web.pokemon.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
class LocalPokemonUpdateIntegrationTest {

    private static final long USER_ID = 1002L;

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final MockMvc mockMvc;
    private final JwtEncoder jwtEncoder;
    private final JdbcTemplate jdbcTemplate;

    @MockitoBean
    private PokemonCatalogPort pokemonCatalogPort;

    @Autowired
    LocalPokemonUpdateIntegrationTest(final MockMvc mockMvc, final JwtEncoder jwtEncoder,
            final JdbcTemplate jdbcTemplate) {
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

    private long sync(final int pokeApiId, final String name) throws Exception {
        when(pokemonCatalogPort.findSummary(name)).thenReturn(Optional.of(new PokemonSummary(pokeApiId, name,
                "https://img/%d.png".formatted(pokeApiId), "Old Category", new BigDecimal("4.0"),
                List.of("pressure", "synchronize"))));
        final var response = mockMvc.perform(post("/api/v1/local/pokemon")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("""
                                { "pokemon": "%s", "localizedName": "old", "region": "Old",
                                  "internalTags": ["old", "legacy"] }""").formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(response).get("id").asLong();
    }

    private List<String> abilitiesOf(final long id) {
        return jdbcTemplate.queryForList(
                "SELECT ability FROM local_pokemon_abilities WHERE local_pokemon_id = ? ORDER BY position", String.class,
                id);
    }

    private List<String> tagsOf(final long id) {
        return jdbcTemplate.queryForList(
                "SELECT tag FROM local_pokemon_internal_tags WHERE local_pokemon_id = ? ORDER BY position", String.class,
                id);
    }

    @Test
    void shouldReplaceEveryFieldButTheIdentifiersWhenPokemonIsUpdated() throws Exception {
        // given
        final var id = sync(150, "mewtwo");

        // when
        mockMvc.perform(put("/api/v1/local/pokemon/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "mewtwo-mega-x", "spriteUrl": "https://img/10043.png",
                                  "category": "Genetic Pokémon", "weightKg": 127.0,
                                  "abilities": ["steadfast"],
                                  "localizedName": "ミュウツー", "region": "Kanto",
                                  "internalTags": ["mega", "legendary"] }"""))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.pokeApiId").value(150))
                .andExpect(jsonPath("$.name").value("mewtwo-mega-x"))
                .andExpect(jsonPath("$.weightKg").value(127.0))
                .andExpect(jsonPath("$.internalTags[0]").value("mega"));

        final var row = jdbcTemplate.queryForMap("SELECT * FROM local_pokemon WHERE id = ?", id);
        assertThat(row).containsEntry("POKE_API_ID", 150)
                .containsEntry("NAME", "mewtwo-mega-x")
                .containsEntry("SPRITE_URL", "https://img/10043.png")
                .containsEntry("CATEGORY", "Genetic Pokémon")
                .containsEntry("WEIGHT_KG", new BigDecimal("127.0"))
                .containsEntry("LOCALIZED_NAME", "ミュウツー")
                .containsEntry("REGION", "Kanto");
        assertThat(abilitiesOf(id)).containsExactly("steadfast");
        assertThat(tagsOf(id)).containsExactly("mega", "legendary");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM local_pokemon WHERE poke_api_id = 150",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void shouldClearOptionalFieldsWhenTheyAreAbsentFromTheBody() throws Exception {
        // given
        final var id = sync(144, "articuno");

        // when
        mockMvc.perform(put("/api/v1/local/pokemon/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"articuno\",\"weightKg\":55.4,\"abilities\":[\"pressure\"]}"))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spriteUrl").value(nullValue()))
                .andExpect(jsonPath("$.category").value(nullValue()))
                .andExpect(jsonPath("$.localizedName").value(nullValue()))
                .andExpect(jsonPath("$.region").value(nullValue()))
                .andExpect(jsonPath("$.internalTags").isEmpty());

        final var row = jdbcTemplate.queryForMap("SELECT * FROM local_pokemon WHERE id = ?", id);
        assertThat(row).containsEntry("SPRITE_URL", null)
                .containsEntry("CATEGORY", null)
                .containsEntry("LOCALIZED_NAME", null)
                .containsEntry("REGION", null);
        assertThat(tagsOf(id)).isEmpty();
    }

    @Test
    void shouldReturnNotFoundWhenNoRecordHasThatId() throws Exception {
        // when & then
        mockMvc.perform(put("/api/v1/local/pokemon/999999").header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"pikachu\",\"weightKg\":6.0,\"abilities\":[\"static\"]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Local Pokemon not found"));
    }
}
