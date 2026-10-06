package com.tech.challenge.web.pokemon.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.tech.challenge.application.pokemon.port.in.DeleteLocalPokemonUseCase;
import com.tech.challenge.application.pokemon.port.in.GetLocalPokemonUseCase;
import com.tech.challenge.application.pokemon.port.in.ListLocalPokemonUseCase;
import com.tech.challenge.application.pokemon.port.in.SyncPokemonUseCase;
import com.tech.challenge.application.pokemon.port.in.UpdateLocalPokemonUseCase;
import com.tech.challenge.application.pokemon.result.LocalPokemonResult;
import com.tech.challenge.application.shared.pagination.PageResult;
import com.tech.challenge.domain.pokemon.exception.LocalPokemonNotFoundException;
import com.tech.challenge.web.pokemon.mapper.LocalPokemonMapperImpl;
import com.tech.challenge.web.shared.security.SecurityConfig;

@WebMvcTest(LocalPokemonController.class)
@Import({ LocalPokemonMapperImpl.class, SecurityConfig.class })
class LocalPokemonReadDeleteControllerTest {

    private static final LocalPokemonResult PIKACHU = new LocalPokemonResult(10L, 25, "pikachu",
            "https://img/25.png", "Mouse Pokémon", new BigDecimal("6.0"), List.of("static", "lightning-rod"),
            "ピカチュウ", "Kanto", List.of("starter", "electric"));

    private final MockMvc mockMvc;
    private final JwtEncoder jwtEncoder;

    @MockitoBean
    private ListLocalPokemonUseCase listLocalPokemonUseCase;

    @MockitoBean
    private GetLocalPokemonUseCase getLocalPokemonUseCase;

    @MockitoBean
    private DeleteLocalPokemonUseCase deleteLocalPokemonUseCase;

    @MockitoBean
    private SyncPokemonUseCase syncPokemonUseCase;

    @MockitoBean
    private UpdateLocalPokemonUseCase updateLocalPokemonUseCase;

    @Autowired
    LocalPokemonReadDeleteControllerTest(final MockMvc mockMvc, final JwtEncoder jwtEncoder) {
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

    private MockHttpServletRequestBuilder authenticated(final MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token());
    }

    @Test
    void shouldReturnPageInContractShapeWhenListIsRequested() throws Exception {
        // given
        when(listLocalPokemonUseCase.list(1L, 1, 2)).thenReturn(PageResult.of(List.of(PIKACHU), 1, 2, 3));

        // when & then
        mockMvc.perform(authenticated(get("/api/v1/local/pokemon").param("page", "1").param("size", "2")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].pokeApiId").value(25))
                .andExpect(jsonPath("$.content[0].name").value("pikachu"))
                .andExpect(jsonPath("$.content[0].spriteUrl").value("https://img/25.png"))
                .andExpect(jsonPath("$.content[0].category").value("Mouse Pokémon"))
                .andExpect(jsonPath("$.content[0].weightKg").value(6.0))
                .andExpect(jsonPath("$.content[0].abilities[1]").value("lightning-rod"))
                .andExpect(jsonPath("$.content[0].localizedName").value("ピカチュウ"))
                .andExpect(jsonPath("$.content[0].region").value("Kanto"))
                .andExpect(jsonPath("$.content[0].internalTags[0]").value("starter"));
    }

    @Test
    void shouldUseDefaultPageAndSizeWhenParametersAreAbsent() throws Exception {
        // given
        when(listLocalPokemonUseCase.list(1L, 0, 20)).thenReturn(PageResult.of(List.of(), 0, 20, 0));

        // when & then
        mockMvc.perform(authenticated(get("/api/v1/local/pokemon")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));

        verify(listLocalPokemonUseCase).list(1L, 0, 20);
    }

    @ParameterizedTest
    @ValueSource(strings = { "page=-1", "size=0", "size=101", "page=abc" })
    void shouldReturnBadRequestWhenPaginationIsOutOfRange(final String query) throws Exception {
        // when & then
        mockMvc.perform(authenticated(get("/api/v1/local/pokemon?" + query)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(listLocalPokemonUseCase);
    }

    @Test
    void shouldListFieldErrorsWhenPaginationIsInvalid() throws Exception {
        // when & then
        mockMvc.perform(authenticated(get("/api/v1/local/pokemon?page=-1&size=101")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors[?(@.field == 'page')].message")
                        .value("must be greater than or equal to 0"))
                .andExpect(jsonPath("$.errors[?(@.field == 'size')].message")
                        .value("must be less than or equal to 100"));
    }

    @Test
    void shouldReturnStoredPokemonWhenIdExists() throws Exception {
        // given
        when(getLocalPokemonUseCase.get(1L, 10L)).thenReturn(PIKACHU);

        // when & then
        mockMvc.perform(authenticated(get("/api/v1/local/pokemon/10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.pokeApiId").value(25))
                .andExpect(jsonPath("$.name").value("pikachu"))
                .andExpect(jsonPath("$.internalTags[1]").value("electric"));
    }

    @Test
    void shouldReturnNotFoundProblemWhenGettingAnUnknownId() throws Exception {
        // given
        when(getLocalPokemonUseCase.get(1L, 99L)).thenThrow(new LocalPokemonNotFoundException(99L));

        // when & then
        mockMvc.perform(authenticated(get("/api/v1/local/pokemon/99")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.instance").value("/api/v1/local/pokemon/99"));
    }

    @Test
    void shouldReturnNoContentWhenStoredPokemonIsDeleted() throws Exception {
        // when & then
        mockMvc.perform(authenticated(delete("/api/v1/local/pokemon/10")))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(deleteLocalPokemonUseCase).delete(1L, 10L);
    }

    @Test
    void shouldReturnNotFoundProblemWhenDeletingAnUnknownId() throws Exception {
        // given
        doThrow(new LocalPokemonNotFoundException(99L)).when(deleteLocalPokemonUseCase).delete(1L, 99L);

        // when & then
        mockMvc.perform(authenticated(delete("/api/v1/local/pokemon/99")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @ParameterizedTest
    @ValueSource(strings = { "0", "-1", "abc" })
    void shouldReturnBadRequestWhenIdIsNotAPositiveNumber(final String id) throws Exception {
        // when & then
        mockMvc.perform(authenticated(get("/api/v1/local/pokemon/" + id)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(authenticated(delete("/api/v1/local/pokemon/" + id)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(getLocalPokemonUseCase, deleteLocalPokemonUseCase);
    }

    private static void expectGenericUnauthorizedProblem(final ResultActions result, final String instance)
            throws Exception {
        result.andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Authentication required"))
                .andExpect(jsonPath("$.instance").value(instance));
    }

    @Test
    void shouldReturnGenericUnauthorizedProblemWhenRequestHasNoToken() throws Exception {
        // when & then
        expectGenericUnauthorizedProblem(mockMvc.perform(get("/api/v1/local/pokemon")), "/api/v1/local/pokemon");
        expectGenericUnauthorizedProblem(mockMvc.perform(get("/api/v1/local/pokemon/10")), "/api/v1/local/pokemon/10");
        expectGenericUnauthorizedProblem(mockMvc.perform(delete("/api/v1/local/pokemon/10")), "/api/v1/local/pokemon/10");

        verifyNoInteractions(listLocalPokemonUseCase, getLocalPokemonUseCase, deleteLocalPokemonUseCase);
    }
}
