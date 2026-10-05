package com.tech.challenge.web.pokemon.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tech.challenge.application.pokemon.port.in.GetPokemonDetailsUseCase;
import com.tech.challenge.application.pokemon.port.in.ListPokemonUseCase;
import com.tech.challenge.application.shared.pagination.PageResult;
import com.tech.challenge.domain.pokemon.exception.PokemonNotFoundException;
import com.tech.challenge.domain.pokemon.model.EvolutionStage;
import com.tech.challenge.domain.pokemon.model.PokemonDetails;
import com.tech.challenge.domain.pokemon.model.PokemonStat;
import com.tech.challenge.domain.pokemon.model.PokemonSummary;
import com.tech.challenge.domain.shared.exception.ExternalServiceUnavailableException;
import com.tech.challenge.web.pokemon.mapper.PokemonDetailsResponseMapperImpl;
import com.tech.challenge.web.shared.security.SecurityConfig;

@WebMvcTest(PokemonCatalogController.class)
@Import({ PokemonDetailsResponseMapperImpl.class, SecurityConfig.class })
class PokemonCatalogControllerTest {

    private static final PokemonDetails BULBASAUR = new PokemonDetails(1, "bulbasaur", "https://img/1.png",
            List.of(new PokemonStat("hp", 45), new PokemonStat("attack", 49)),
            "A strange seed was planted on its back at birth.",
            new EvolutionStage("bulbasaur", "https://img/s1.png", List.of(
                    new EvolutionStage("ivysaur", "https://img/s2.png", List.of(
                            new EvolutionStage("venusaur", null, List.of()))))));

    private static final PokemonSummary BULBASAUR_SUMMARY = new PokemonSummary(1, "bulbasaur", "https://img/1.png",
            "Seed Pokémon", new BigDecimal("6.9"), List.of("overgrow", "chlorophyll"));

    private final MockMvc mockMvc;

    @MockitoBean
    private GetPokemonDetailsUseCase getPokemonDetailsUseCase;

    @MockitoBean
    private ListPokemonUseCase listPokemonUseCase;

    @Autowired
    PokemonCatalogControllerTest(final MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void shouldReturnDetailsInContractShapeWhenPokemonExists() throws Exception {
        // given
        when(getPokemonDetailsUseCase.getDetails("bulbasaur")).thenReturn(BULBASAUR);

        // when & then
        mockMvc.perform(get("/api/v1/pokemon/{idOrName}", "bulbasaur"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("bulbasaur"))
                .andExpect(jsonPath("$.imageUrl").value("https://img/1.png"))
                .andExpect(jsonPath("$.stats[0].name").value("hp"))
                .andExpect(jsonPath("$.stats[0].baseStat").value(45))
                .andExpect(jsonPath("$.stats[1].name").value("attack"))
                .andExpect(jsonPath("$.description").value("A strange seed was planted on its back at birth."))
                .andExpect(jsonPath("$.evolutionChain.name").value("bulbasaur"))
                .andExpect(jsonPath("$.evolutionChain.evolvesTo[0].name").value("ivysaur"))
                .andExpect(jsonPath("$.evolutionChain.evolvesTo[0].evolvesTo[0].name").value("venusaur"))
                .andExpect(jsonPath("$.evolutionChain.evolvesTo[0].evolvesTo[0].evolvesTo").isEmpty());
    }

    @Test
    void shouldReturnSpriteUrlOfEveryStageWhenEvolutionChainHasSprites() throws Exception {
        // given
        when(getPokemonDetailsUseCase.getDetails("bulbasaur")).thenReturn(BULBASAUR);

        // when & then
        mockMvc.perform(get("/api/v1/pokemon/{idOrName}", "bulbasaur"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evolutionChain.spriteUrl").value("https://img/s1.png"))
                .andExpect(jsonPath("$.evolutionChain.evolvesTo[0].spriteUrl").value("https://img/s2.png"))
                .andExpect(content().string(containsString("{\"name\":\"venusaur\",\"spriteUrl\":null,\"evolvesTo\":[]}")));
    }

    @Test
    void shouldBePublicWhenRequestHasNoToken() throws Exception {
        // given
        when(getPokemonDetailsUseCase.getDetails("1")).thenReturn(BULBASAUR);

        // when & then
        mockMvc.perform(get("/api/v1/pokemon/{idOrName}", "1")).andExpect(status().isOk());
    }

    @Test
    void shouldReturnNotFoundProblemWhenPokemonDoesNotExist() throws Exception {
        // given
        when(getPokemonDetailsUseCase.getDetails("missingno")).thenThrow(new PokemonNotFoundException("missingno"));

        // when & then
        mockMvc.perform(get("/api/v1/pokemon/{idOrName}", "missingno"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Pokemon not found"))
                .andExpect(jsonPath("$.instance").value("/api/v1/pokemon/missingno"));
    }

    @Test
    void shouldReturnBadGatewayProblemWithoutInternalsWhenPokeApiIsUnavailable() throws Exception {
        // given
        when(getPokemonDetailsUseCase.getDetails("1")).thenThrow(
                new ExternalServiceUnavailableException("PokeAPI is unavailable", new IllegalStateException("secret upstream body")));

        // when & then
        mockMvc.perform(get("/api/v1/pokemon/{idOrName}", "1"))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Bad Gateway"))
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.detail").value("PokeAPI is unavailable"))
                .andExpect(jsonPath("$.instance").value("/api/v1/pokemon/1"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString("secret upstream body"))))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void shouldReturnBadRequestProblemWhenIdOrNameIsBlank() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/pokemon/{idOrName}", " "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("idOrName"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));

        verify(getPokemonDetailsUseCase, never()).getDetails(any());
    }

    @Test
    void shouldReturnNotFoundProblemWhenRouteDoesNotExist() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/pokemon/{idOrName}/unknown", "1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturnPageInContractShapeWhenListIsRequested() throws Exception {
        // given
        when(listPokemonUseCase.list(0, 20)).thenReturn(PageResult.of(List.of(BULBASAUR_SUMMARY), 0, 20, 1302));

        // when & then
        mockMvc.perform(get("/api/v1/pokemon").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("bulbasaur"))
                .andExpect(jsonPath("$.content[0].spriteUrl").value("https://img/1.png"))
                .andExpect(jsonPath("$.content[0].category").value("Seed Pokémon"))
                .andExpect(jsonPath("$.content[0].weightKg").value(6.9))
                .andExpect(jsonPath("$.content[0].abilities[0]").value("overgrow"))
                .andExpect(jsonPath("$.content[0].abilities[1]").value("chlorophyll"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1302))
                .andExpect(jsonPath("$.totalPages").value(66));
    }

    @Test
    void shouldUseFirstPageOfTwentyWhenPaginationParametersAreMissing() throws Exception {
        // given
        when(listPokemonUseCase.list(0, 20)).thenReturn(PageResult.of(List.of(), 0, 20, 1302));

        // when & then
        mockMvc.perform(get("/api/v1/pokemon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));

        verify(listPokemonUseCase).list(0, 20);
    }

    @Test
    void shouldBePublicWhenListRequestHasNoToken() throws Exception {
        // given
        when(listPokemonUseCase.list(2, 100)).thenReturn(PageResult.of(List.of(), 2, 100, 1302));

        // when & then
        mockMvc.perform(get("/api/v1/pokemon").param("page", "2").param("size", "100")).andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource({
            "page, -1, must be greater than or equal to 0",
            "size, 0, must be greater than or equal to 1",
            "size, 101, must be less than or equal to 100",
            "page, abc, must be an integer",
            "size, 1.5, must be an integer" })
    void shouldReturnBadRequestProblemWhenPaginationParameterIsInvalid(final String parameter, final String value,
            final String message) throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/pokemon").param(parameter, value))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.instance").value("/api/v1/pokemon"))
                .andExpect(jsonPath("$.errors[0].field").value(parameter))
                .andExpect(jsonPath("$.errors[0].message").value(message))
                .andExpect(content().string(not(containsString("Exception"))));

        verify(listPokemonUseCase, never()).list(anyInt(), anyInt());
    }

    @Test
    void shouldReturnBadGatewayProblemWhenPokeApiIsUnavailableForList() throws Exception {
        // given
        when(listPokemonUseCase.list(0, 20)).thenThrow(
                new ExternalServiceUnavailableException("PokeAPI is unavailable", new IllegalStateException("secret upstream body")));

        // when & then
        mockMvc.perform(get("/api/v1/pokemon"))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.detail").value("PokeAPI is unavailable"))
                .andExpect(jsonPath("$.instance").value("/api/v1/pokemon"))
                .andExpect(content().string(not(containsString("secret upstream body"))));
    }
}
