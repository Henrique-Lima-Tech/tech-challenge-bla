package com.tech.challenge.web.pokemon.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
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

import com.tech.challenge.application.pokemon.command.UpdateLocalPokemonCommand;
import com.tech.challenge.application.pokemon.port.in.DeleteLocalPokemonUseCase;
import com.tech.challenge.application.pokemon.port.in.GetLocalPokemonUseCase;
import com.tech.challenge.application.pokemon.port.in.ListLocalPokemonUseCase;
import com.tech.challenge.application.pokemon.port.in.SyncPokemonUseCase;
import com.tech.challenge.application.pokemon.port.in.UpdateLocalPokemonUseCase;
import com.tech.challenge.application.pokemon.result.LocalPokemonResult;
import com.tech.challenge.domain.pokemon.exception.LocalPokemonNotFoundException;
import com.tech.challenge.web.pokemon.mapper.LocalPokemonMapperImpl;
import com.tech.challenge.web.shared.security.SecurityConfig;

import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(LocalPokemonController.class)
@Import({ LocalPokemonMapperImpl.class, SecurityConfig.class })
class LocalPokemonUpdateControllerTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final BigDecimal WEIGHT = new BigDecimal("6.0");
    private static final LocalPokemonResult PIKACHU = new LocalPokemonResult(10L, 25, "pikachu",
            "https://img/25.png", "Mouse Pokémon", WEIGHT, List.of("static", "lightning-rod"), "Pikachu", "Kanto",
            List.of("mascot"));

    private final MockMvc mockMvc;
    private final JwtEncoder jwtEncoder;

    @MockitoBean
    private UpdateLocalPokemonUseCase updateLocalPokemonUseCase;

    @MockitoBean
    private SyncPokemonUseCase syncPokemonUseCase;

    @MockitoBean
    private ListLocalPokemonUseCase listLocalPokemonUseCase;

    @MockitoBean
    private GetLocalPokemonUseCase getLocalPokemonUseCase;

    @MockitoBean
    private DeleteLocalPokemonUseCase deleteLocalPokemonUseCase;

    @Autowired
    LocalPokemonUpdateControllerTest(final MockMvc mockMvc, final JwtEncoder jwtEncoder) {
        this.mockMvc = mockMvc;
        this.jwtEncoder = jwtEncoder;
    }

    private String token() {
        final var now = Instant.now();
        final var claims = JwtClaimsSet.builder()
                .subject("1")
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofHours(1)))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private MockHttpServletRequestBuilder authenticatedPut(final String path, final String body) {
        return put(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private MockHttpServletRequestBuilder authenticatedPut(final String body) {
        return authenticatedPut("/api/v1/local/pokemon/10", body);
    }

    private static LinkedHashMap<String, Object> validBody() {
        final var body = new LinkedHashMap<String, Object>();
        body.put("name", "pikachu");
        body.put("spriteUrl", "https://img/25.png");
        body.put("category", "Mouse Pokémon");
        body.put("weightKg", WEIGHT);
        body.put("abilities", List.of("static", "lightning-rod"));
        body.put("localizedName", "Pikachu");
        body.put("region", "Kanto");
        body.put("internalTags", List.of("mascot"));
        return body;
    }

    private static String bodyWith(final String field, final Object value) {
        final var body = validBody();
        body.put(field, value);
        return JSON.writeValueAsString(body);
    }

    private static String bodyWithout(final String... fields) {
        final var body = validBody();
        Arrays.asList(fields).forEach(body::remove);
        return JSON.writeValueAsString(body);
    }

    private static List<String> texts(final String prefix, final int count, final int length) {
        return IntStream.range(0, count)
                .mapToObj(i -> prefix.repeat(length - 2) + "%02d".formatted(i))
                .toList();
    }

    @Test
    void shouldReturnUpdatedPokemonInContractShapeWhenUpdateSucceeds() throws Exception {
        // given
        when(updateLocalPokemonUseCase.update(new UpdateLocalPokemonCommand(1L, 10L, "pikachu", "https://img/25.png",
                "Mouse Pokémon", WEIGHT, List.of("static", "lightning-rod"), "Pikachu", "Kanto", List.of("mascot"))))
                .thenReturn(PIKACHU);

        // when & then
        mockMvc.perform(authenticatedPut(JSON.writeValueAsString(validBody())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.pokeApiId").value(25))
                .andExpect(jsonPath("$.name").value("pikachu"))
                .andExpect(jsonPath("$.spriteUrl").value("https://img/25.png"))
                .andExpect(jsonPath("$.category").value("Mouse Pokémon"))
                .andExpect(jsonPath("$.weightKg").value(6.0))
                .andExpect(jsonPath("$.abilities", contains("static", "lightning-rod")))
                .andExpect(jsonPath("$.localizedName").value("Pikachu"))
                .andExpect(jsonPath("$.region").value("Kanto"))
                .andExpect(jsonPath("$.internalTags", contains("mascot")));
    }

    @Test
    void shouldReplaceOptionalFieldsWithNullWhenTheyAreAbsent() throws Exception {
        // given
        when(updateLocalPokemonUseCase.update(any())).thenReturn(PIKACHU);

        // when
        final var result = mockMvc.perform(
                authenticatedPut(bodyWithout("spriteUrl", "category", "localizedName", "region", "internalTags")));

        // then
        result.andExpect(status().isOk());
        verify(updateLocalPokemonUseCase).update(new UpdateLocalPokemonCommand(1L, 10L, "pikachu", null, null, WEIGHT,
                List.of("static", "lightning-rod"), null, null, null));
    }

    @Test
    void shouldTrimEveryTextFieldWhenUpdating() throws Exception {
        // given
        when(updateLocalPokemonUseCase.update(any())).thenReturn(PIKACHU);
        final var body = validBody();
        body.put("name", "  pikachu ");
        body.put("spriteUrl", " https://img/25.png ");
        body.put("category", " Mouse Pokémon ");
        body.put("abilities", List.of(" static", "lightning-rod  "));
        body.put("localizedName", " Pikachu ");
        body.put("region", " Kanto ");
        body.put("internalTags", List.of("  mascot  "));

        // when
        final var result = mockMvc.perform(authenticatedPut(JSON.writeValueAsString(body)));

        // then
        result.andExpect(status().isOk());
        verify(updateLocalPokemonUseCase).update(new UpdateLocalPokemonCommand(1L, 10L, "pikachu", "https://img/25.png",
                "Mouse Pokémon", WEIGHT, List.of("static", "lightning-rod"), "Pikachu", "Kanto", List.of("mascot")));
    }

    @Test
    void shouldAcceptFieldsWhenTheyAreAtTheirLimits() throws Exception {
        // given
        when(updateLocalPokemonUseCase.update(any())).thenReturn(PIKACHU);
        final var body = validBody();
        body.put("name", "p".repeat(50));
        body.put("spriteUrl", "https://img/" + "a".repeat(488));
        body.put("category", "c".repeat(50));
        body.put("weightKg", new BigDecimal("9999.9"));
        body.put("abilities", texts("a", 10, 50));
        body.put("localizedName", "l".repeat(100));
        body.put("region", "r".repeat(100));
        body.put("internalTags", texts("t", 20, 30));

        // when & then
        mockMvc.perform(authenticatedPut(JSON.writeValueAsString(body))).andExpect(status().isOk());
    }

    @Test
    void shouldAcceptZeroWeightWhenItIsSent() throws Exception {
        // given
        when(updateLocalPokemonUseCase.update(any())).thenReturn(PIKACHU);

        // when & then
        mockMvc.perform(authenticatedPut(bodyWith("weightKg", new BigDecimal("0.0")))).andExpect(status().isOk());
    }

    @Test
    void shouldIgnoreUnknownPropertyWhenItIsNotAnIdentifier() throws Exception {
        // given
        when(updateLocalPokemonUseCase.update(any())).thenReturn(PIKACHU);

        // when & then
        mockMvc.perform(authenticatedPut(bodyWith("nmae", "typo"))).andExpect(status().isOk());
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("name", bodyWithout("name"), "must not be blank"),
                Arguments.of("name", bodyWith("name", "   "), "must not be blank"),
                Arguments.of("name", bodyWith("name", "p".repeat(51)), "size must be at most 50"),
                Arguments.of("spriteUrl", bodyWith("spriteUrl", "ftp://img/25.png"),
                        "must be a valid http or https URL"),
                Arguments.of("spriteUrl", bodyWith("spriteUrl", "not a url"), "must be a valid http or https URL"),
                Arguments.of("spriteUrl", bodyWith("spriteUrl", "https:///25.png"),
                        "must be a valid http or https URL"),
                Arguments.of("spriteUrl", bodyWith("spriteUrl", "   "), "must be a valid http or https URL"),
                Arguments.of("spriteUrl", bodyWith("spriteUrl", "https://img/" + "a".repeat(489)),
                        "size must be at most 500"),
                Arguments.of("category", bodyWith("category", "c".repeat(51)), "size must be at most 50"),
                Arguments.of("weightKg", bodyWithout("weightKg"), "must not be null"),
                Arguments.of("weightKg", bodyWith("weightKg", new BigDecimal("-0.1")),
                        "must be greater than or equal to 0"),
                Arguments.of("weightKg", bodyWith("weightKg", new BigDecimal("10000.0")),
                        "must be less than or equal to 9999.9"),
                Arguments.of("weightKg", bodyWith("weightKg", new BigDecimal("6.05")),
                        "must have at most 1 decimal place"),
                Arguments.of("weightKg", bodyWith("weightKg", new BigDecimal("6.00")),
                        "must have at most 1 decimal place"),
                Arguments.of("abilities", bodyWithout("abilities"), "must not be null"),
                Arguments.of("abilities", bodyWith("abilities", List.of()), "size must be between 1 and 10"),
                Arguments.of("abilities", bodyWith("abilities", texts("a", 11, 50)),
                        "size must be between 1 and 10"),
                Arguments.of("abilities[1]", bodyWith("abilities", List.of("static", "  ")), "must not be blank"),
                Arguments.of("abilities[0]", bodyWith("abilities", Arrays.asList((String) null)),
                        "must not be blank"),
                Arguments.of("abilities[0]", bodyWith("abilities", List.of("a".repeat(51))),
                        "size must be at most 50"),
                Arguments.of("abilities", bodyWith("abilities", List.of("static", "static")),
                        "must not contain duplicates"),
                Arguments.of("localizedName", bodyWith("localizedName", "l".repeat(101)),
                        "size must be at most 100"),
                Arguments.of("region", bodyWith("region", "r".repeat(101)), "size must be at most 100"),
                Arguments.of("internalTags", bodyWith("internalTags", texts("t", 21, 30)),
                        "size must be at most 20"),
                Arguments.of("internalTags[1]", bodyWith("internalTags", List.of("mascot", "  ")),
                        "must not be blank"),
                Arguments.of("internalTags[0]", bodyWith("internalTags", Arrays.asList((String) null)),
                        "must not be blank"),
                Arguments.of("internalTags[0]", bodyWith("internalTags", List.of("t".repeat(31))),
                        "size must be at most 30"),
                Arguments.of("internalTags", bodyWith("internalTags", List.of("mascot", "mascot")),
                        "must not contain duplicates"),
                Arguments.of("internalTags", bodyWith("internalTags", List.of("Mascot", " mascot")),
                        "must not contain duplicates"),
                Arguments.of("id", bodyWith("id", 10), "must not be sent"),
                Arguments.of("pokeApiId", bodyWith("pokeApiId", 25), "must not be sent"));
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void shouldReturnBadRequestProblemWhenRequestIsInvalid(final String field, final String body, final String message)
            throws Exception {
        // when & then
        mockMvc.perform(authenticatedPut(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.instance").value("/api/v1/local/pokemon/10"))
                .andExpect(jsonPath("$.errors[?(@.field == '" + field + "')].message").value(hasItem(message)));

        verifyNoInteractions(updateLocalPokemonUseCase);
    }

    @Test
    void shouldReturnBadRequestProblemWhenBodyIsMalformed() throws Exception {
        // when & then
        mockMvc.perform(authenticatedPut("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Failed to read request"))
                .andExpect(content().string(not(containsString("Exception"))));

        verifyNoInteractions(updateLocalPokemonUseCase);
    }

    @Test
    void shouldReturnBadRequestProblemWhenBodyIsEmpty() throws Exception {
        // when & then
        mockMvc.perform(authenticatedPut(""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Failed to read request"));

        verifyNoInteractions(updateLocalPokemonUseCase);
    }

    @ParameterizedTest
    @CsvSource({ "abc, must be an integer", "1.5, must be an integer", "0, must be greater than 0",
            "-1, must be greater than 0" })
    void shouldReturnBadRequestProblemWhenPathIdIsNotAPositiveNumber(final String pathId, final String message)
            throws Exception {
        // when & then
        mockMvc.perform(authenticatedPut("/api/v1/local/pokemon/" + pathId, JSON.writeValueAsString(validBody())))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors[?(@.field == 'id')].message").value(hasItem(message)));

        verifyNoInteractions(updateLocalPokemonUseCase);
    }

    @Test
    void shouldReturnBadRequestBeforeLookingForTheRecordWhenBodyIsInvalidAndIdIsUnknown() throws Exception {
        // when & then
        mockMvc.perform(authenticatedPut("/api/v1/local/pokemon/99999", bodyWithout("name")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"));

        verifyNoInteractions(updateLocalPokemonUseCase);
    }

    @Test
    void shouldReturnNotFoundProblemWhenRecordDoesNotExist() throws Exception {
        // given
        when(updateLocalPokemonUseCase.update(any())).thenThrow(new LocalPokemonNotFoundException(99L));

        // when & then
        mockMvc.perform(authenticatedPut("/api/v1/local/pokemon/99", JSON.writeValueAsString(validBody())))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Local Pokemon not found"))
                .andExpect(jsonPath("$.instance").value("/api/v1/local/pokemon/99"));
    }

    @Test
    void shouldReturnUnauthorizedProblemWhenRequestHasNoToken() throws Exception {
        // when & then
        mockMvc.perform(put("/api/v1/local/pokemon/10").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(validBody())))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Authentication required"))
                .andExpect(jsonPath("$.instance").value("/api/v1/local/pokemon/10"));

        verifyNoInteractions(updateLocalPokemonUseCase);
    }

    @Test
    void shouldKeepListOrderWhenAbilitiesAndTagsAreSent() throws Exception {
        // given
        when(updateLocalPokemonUseCase.update(any())).thenReturn(PIKACHU);
        final var body = validBody();
        body.put("abilities", List.of("zeta", "alpha", "mid"));
        body.put("internalTags", List.of("zeta", "alpha", "mid"));

        // when
        final var result = mockMvc.perform(authenticatedPut(JSON.writeValueAsString(body)));

        // then
        result.andExpect(status().isOk());
        verify(updateLocalPokemonUseCase).update(new UpdateLocalPokemonCommand(1L, 10L, "pikachu", "https://img/25.png",
                "Mouse Pokémon", WEIGHT, List.of("zeta", "alpha", "mid"), "Pikachu", "Kanto",
                List.of("zeta", "alpha", "mid")));
    }
}
