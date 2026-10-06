package com.tech.challenge.web.shared.security;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.tech.challenge.application.pokemon.port.in.GetPokemonDetailsUseCase;
import com.tech.challenge.application.pokemon.port.in.ListPokemonUseCase;
import com.tech.challenge.application.shared.pagination.PageResult;
import com.tech.challenge.web.pokemon.controller.PokemonCatalogController;
import com.tech.challenge.web.pokemon.mapper.PokemonDetailsResponseMapperImpl;

@WebMvcTest(PokemonCatalogController.class)
@Import({ PokemonDetailsResponseMapperImpl.class, SecurityConfig.class })
class SecurityConfigTest {

    private final MockMvc mockMvc;
    private final JwtEncoder jwtEncoder;

    @MockitoBean
    private GetPokemonDetailsUseCase getPokemonDetailsUseCase;

    @MockitoBean
    private ListPokemonUseCase listPokemonUseCase;

    @Autowired
    SecurityConfigTest(final MockMvc mockMvc, final JwtEncoder jwtEncoder) {
        this.mockMvc = mockMvc;
        this.jwtEncoder = jwtEncoder;
    }

    private String token(final Instant issuedAt, final Duration lifetime) {
        final var claims = JwtClaimsSet.builder()
                .subject("1")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(lifetime))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private static void expectGenericUnauthorizedProblem(final ResultActions result) throws Exception {
        result.andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Authentication required"))
                .andExpect(jsonPath("$.instance").value("/api/v1/local/pokemon"));
    }

    @Test
    void shouldReturnUnauthorizedWhenRouteIsNotPublicAndRequestHasNoToken() throws Exception {
        // when
        final var result = mockMvc.perform(get("/api/v1/local/pokemon"));

        // then
        expectGenericUnauthorizedProblem(result);
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenIsMalformed() throws Exception {
        // when
        final var result = mockMvc.perform(get("/api/v1/local/pokemon").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"));

        // then
        expectGenericUnauthorizedProblem(result);
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenSignatureIsInvalid() throws Exception {
        // given
        final var valid = token(Instant.now(), Duration.ofHours(1));
        final var tampered = valid.substring(0, valid.lastIndexOf('.') + 1) + "invalidsignature";

        // when
        final var result = mockMvc.perform(get("/api/v1/local/pokemon").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered));

        // then
        expectGenericUnauthorizedProblem(result);
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenIsExpired() throws Exception {
        // given
        final var expired = token(Instant.now().minus(Duration.ofHours(2)), Duration.ofHours(1));

        // when
        final var result = mockMvc.perform(get("/api/v1/local/pokemon").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired));

        // then
        expectGenericUnauthorizedProblem(result);
    }

    @Test
    void shouldPassSecurityWhenTokenIsValid() throws Exception {
        // given
        final var valid = token(Instant.now(), Duration.ofHours(1));

        // when & then
        mockMvc.perform(get("/api/v1/local/pokemon").header(HttpHeaders.AUTHORIZATION, "Bearer " + valid))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = { "Bearer not-a-jwt", "Bearer " })
    void shouldIgnoreTokenWhenRouteIsPublic(final String authorization) throws Exception {
        // given
        when(listPokemonUseCase.list(0, 20)).thenReturn(PageResult.of(List.of(), 0, 20, 0));

        // when & then
        mockMvc.perform(get("/api/v1/pokemon").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectRequestWhenPathContainsLineBreak() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/pokemon/{idOrName}", "pikachu\nFAKE LOG LINE"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(getPokemonDetailsUseCase);
    }
}
