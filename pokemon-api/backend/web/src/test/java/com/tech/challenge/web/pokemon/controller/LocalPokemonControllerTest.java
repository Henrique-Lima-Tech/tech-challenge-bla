package com.tech.challenge.web.pokemon.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.tech.challenge.application.pokemon.command.SyncPokemonCommand;
import com.tech.challenge.application.pokemon.port.in.DeleteLocalPokemonUseCase;
import com.tech.challenge.application.pokemon.port.in.GetLocalPokemonUseCase;
import com.tech.challenge.application.pokemon.port.in.ListLocalPokemonUseCase;
import com.tech.challenge.application.pokemon.port.in.SyncPokemonUseCase;
import com.tech.challenge.application.pokemon.port.in.UpdateLocalPokemonUseCase;
import com.tech.challenge.application.pokemon.result.LocalPokemonResult;
import com.tech.challenge.domain.pokemon.exception.PokemonAlreadySyncedException;
import com.tech.challenge.domain.pokemon.exception.PokemonNotFoundException;
import com.tech.challenge.domain.shared.exception.ExternalServiceUnavailableException;
import com.tech.challenge.web.pokemon.mapper.LocalPokemonMapperImpl;
import com.tech.challenge.web.shared.security.SecurityConfig;

import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(LocalPokemonController.class)
@Import({ LocalPokemonMapperImpl.class, SecurityConfig.class })
class LocalPokemonControllerTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final LocalPokemonResult PIKACHU = new LocalPokemonResult(10L, 25, "pikachu",
            "https://img/25.png", "Mouse Pokémon", new BigDecimal("6.0"), List.of("static", "lightning-rod"),
            "ピカチュウ", "Kanto", List.of("starter", "electric"));

    private final MockMvc mockMvc;
    private final JwtEncoder jwtEncoder;

    @MockitoBean
    private SyncPokemonUseCase syncPokemonUseCase;

    // Required by the controller constructor; unused by the sync tests.
    @MockitoBean
    private UpdateLocalPokemonUseCase updateLocalPokemonUseCase;

    @MockitoBean
    private ListLocalPokemonUseCase listLocalPokemonUseCase;

    @MockitoBean
    private GetLocalPokemonUseCase getLocalPokemonUseCase;

    @MockitoBean
    private DeleteLocalPokemonUseCase deleteLocalPokemonUseCase;

    @Autowired
    LocalPokemonControllerTest(final MockMvc mockMvc, final JwtEncoder jwtEncoder) {
        this.mockMvc = mockMvc;
        this.jwtEncoder = jwtEncoder;
    }

    private MockHttpServletRequestBuilder authenticatedPost(final String body) {
        final var now = Instant.now();
        final var claims = JwtClaimsSet.builder()
                .subject("1")
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofHours(1)))
                .build();
        final var token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return post("/api/v1/local/pokemon").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static String body(final String pokemon, final String localizedName, final String region,
            final List<String> internalTags) {
        final var body = new LinkedHashMap<String, Object>();
        body.put("pokemon", pokemon);
        body.put("localizedName", localizedName);
        body.put("region", region);
        body.put("internalTags", internalTags);
        return JSON.writeValueAsString(body);
    }

    private static List<String> tags(final int count) {
        return IntStream.range(0, count).mapToObj(i -> "tag" + i).toList();
    }

    @Test
    void shouldReturnCreatedPokemonInContractShapeWhenSyncSucceeds() throws Exception {
        // given
        when(syncPokemonUseCase.sync(new SyncPokemonCommand(1L, "pikachu", "ピカチュウ", "Kanto",
                List.of("starter", "electric")))).thenReturn(PIKACHU);

        // when & then
        mockMvc.perform(authenticatedPost(body("pikachu", "ピカチュウ", "Kanto", List.of("starter", "electric"))))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/api/v1/local/pokemon/10"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.pokeApiId").value(25))
                .andExpect(jsonPath("$.name").value("pikachu"))
                .andExpect(jsonPath("$.spriteUrl").value("https://img/25.png"))
                .andExpect(jsonPath("$.category").value("Mouse Pokémon"))
                .andExpect(jsonPath("$.weightKg").value(6.0))
                .andExpect(jsonPath("$.abilities", contains("static", "lightning-rod")))
                .andExpect(jsonPath("$.localizedName").value("ピカチュウ"))
                .andExpect(jsonPath("$.region").value("Kanto"))
                .andExpect(jsonPath("$.internalTags", contains("starter", "electric")));
    }

    @Test
    void shouldAcceptRequestWhenOnlyPokemonIsSent() throws Exception {
        // given
        when(syncPokemonUseCase.sync(any())).thenReturn(PIKACHU);

        // when
        final var result = mockMvc.perform(authenticatedPost("{\"pokemon\": \"25\"}"));

        // then
        result.andExpect(status().isCreated());
        verify(syncPokemonUseCase).sync(new SyncPokemonCommand(1L, "25", null, null, null));
    }

    @Test
    void shouldTrimTextFieldsAndEveryTagWhenSyncing() throws Exception {
        // given
        when(syncPokemonUseCase.sync(any())).thenReturn(PIKACHU);

        // when
        final var result = mockMvc.perform(
                authenticatedPost(body("  Pikachu ", " ピカチュウ ", " Kanto ", List.of(" starter ", "electric  "))));

        // then
        result.andExpect(status().isCreated());
        verify(syncPokemonUseCase).sync(new SyncPokemonCommand(1L, "Pikachu", "ピカチュウ", "Kanto",
                List.of("starter", "electric")));
    }

    @Test
    void shouldAcceptFieldsWhenTheyAreAtTheirLimits() throws Exception {
        // given
        final var limitTags = new ArrayList<>(tags(19));
        limitTags.add("t".repeat(30));
        when(syncPokemonUseCase.sync(any())).thenReturn(PIKACHU);

        // when & then
        mockMvc.perform(authenticatedPost(body("p".repeat(50), "l".repeat(100), "r".repeat(100), limitTags)))
                .andExpect(status().isCreated());
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("pokemon", body(null, null, null, null), "must not be blank"),
                Arguments.of("pokemon", body("   ", null, null, null), "must not be blank"),
                Arguments.of("pokemon", body("p".repeat(51), null, null, null), "size must be at most 50"),
                Arguments.of("localizedName", body("pikachu", "l".repeat(101), null, null), "size must be at most 100"),
                Arguments.of("region", body("pikachu", null, "r".repeat(101), null), "size must be at most 100"),
                Arguments.of("internalTags", body("pikachu", null, null, tags(21)), "size must be at most 20"),
                Arguments.of("internalTags[1]", body("pikachu", null, null, List.of("starter", "  ")), "must not be blank"),
                Arguments.of("internalTags[0]", body("pikachu", null, null, Arrays.asList((String) null)), "must not be blank"),
                Arguments.of("internalTags[0]", body("pikachu", null, null, List.of("t".repeat(31))), "size must be at most 30"),
                Arguments.of("internalTags", body("pikachu", null, null, List.of("starter", "starter")), "must not contain duplicates"),
                Arguments.of("internalTags", body("pikachu", null, null, List.of("Starter", " starter")), "must not contain duplicates"));
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void shouldReturnBadRequestProblemWhenRequestIsInvalid(final String field, final String body, final String message)
            throws Exception {
        // when & then
        mockMvc.perform(authenticatedPost(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.instance").value("/api/v1/local/pokemon"))
                .andExpect(jsonPath("$.errors[?(@.field == '" + field + "')].message").value(hasItem(message)));

        verifyNoInteractions(syncPokemonUseCase);
    }

    @Test
    void shouldReturnBadRequestProblemWhenBodyIsMalformed() throws Exception {
        // when & then
        mockMvc.perform(authenticatedPost("{\"pokemon\": "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Failed to read request"))
                .andExpect(content().string(not(containsString("Exception"))));

        verifyNoInteractions(syncPokemonUseCase);
    }

    @Test
    void shouldReturnBadRequestProblemWhenBodyIsEmpty() throws Exception {
        // when & then
        mockMvc.perform(authenticatedPost(""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Failed to read request"));

        verifyNoInteractions(syncPokemonUseCase);
    }

    @Test
    void shouldReturnNotFoundProblemWhenPokeApiDoesNotKnowThePokemon() throws Exception {
        // given
        when(syncPokemonUseCase.sync(any())).thenThrow(new PokemonNotFoundException("missingno"));

        // when & then
        mockMvc.perform(authenticatedPost(body("missingno", null, null, null)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Pokemon not found"))
                .andExpect(jsonPath("$.instance").value("/api/v1/local/pokemon"));
    }

    @Test
    void shouldReturnConflictProblemWhenPokemonIsAlreadySynced() throws Exception {
        // given
        when(syncPokemonUseCase.sync(any())).thenThrow(new PokemonAlreadySyncedException());

        // when & then
        mockMvc.perform(authenticatedPost(body("pikachu", null, null, null)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Pokemon already synced"))
                .andExpect(jsonPath("$.instance").value("/api/v1/local/pokemon"));
    }

    @Test
    void shouldReturnBadGatewayProblemWhenPokeApiIsUnavailable() throws Exception {
        // given
        when(syncPokemonUseCase.sync(any()))
                .thenThrow(new ExternalServiceUnavailableException("PokeAPI is unavailable", null));

        // when & then
        mockMvc.perform(authenticatedPost(body("pikachu", null, null, null)))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.detail").value("PokeAPI is unavailable"))
                .andExpect(jsonPath("$.instance").value("/api/v1/local/pokemon"));
    }

    @Test
    void shouldReturnUnauthorizedProblemWhenRequestHasNoToken() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/local/pokemon").contentType(MediaType.APPLICATION_JSON)
                        .content(body("pikachu", null, null, null)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Authentication required"))
                .andExpect(jsonPath("$.instance").value("/api/v1/local/pokemon"));

        verifyNoInteractions(syncPokemonUseCase);
    }
}
