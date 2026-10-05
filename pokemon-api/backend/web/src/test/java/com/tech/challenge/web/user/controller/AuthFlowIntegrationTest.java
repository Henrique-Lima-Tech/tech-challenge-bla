package com.tech.challenge.web.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.json.JsonMapper;

/**
 * Register, login and a protected route through the real chain: BCrypt, Flyway on H2 in memory, token issuing
 * and the resource server. The database lives as long as the JVM, so each test uses its own emails.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    AuthFlowIntegrationTest(final MockMvc mockMvc, final JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.jdbcTemplate = jdbcTemplate;
    }

    private void register(final String name, final String email, final String password, final int expectedStatus)
            throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}".formatted(name, email, password)))
                .andExpect(status().is(expectedStatus));
    }

    private String login(final String email, final String password, final int expectedStatus) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void shouldReachProtectedRouteWhenRegisteredUserLogsIn() throws Exception {
        // given
        register("Ash", "Ash@Example.com", "pikachu123", 201);
        final var token = JSON.readTree(login("ash@example.com", "pikachu123", 200)).get("accessToken").asString();

        // when & then
        // Body validation runs only after security accepted the token: an empty JSON object ends in 400, not 401.
        mockMvc.perform(post("/api/v1/local/pokemon").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnTheUsersNameWhenTheUserLogsIn() throws Exception {
        // given
        register("Erika", "erika@example.com", "tangela12", 201);

        // when
        final var response = JSON.readTree(login("erika@example.com", "tangela12", 200));

        // then
        assertThat(response.get("name").asString()).isEqualTo("Erika");
    }

    @Test
    void shouldStoreOnlyBcryptHashWhenUserRegisters() throws Exception {
        // when
        register("Brock", "brock@example.com", "onix-rocks", 201);

        // then
        final var hash = jdbcTemplate.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class,
                "brock@example.com");
        assertThat(hash).startsWith("$2a$").doesNotContain("onix-rocks");
    }

    @Test
    void shouldReturnConflictWhenEmailDiffersOnlyInCase() throws Exception {
        // given
        register("Misty", "misty@example.com", "starmie123", 201);

        // when & then
        register("Misty", "MISTY@example.com", "starmie123", 409);
    }

    @Test
    void shouldAnswerIdenticallyWhenEmailIsUnknownOrPasswordIsWrong() throws Exception {
        // given
        register("Gary", "gary@example.com", "eevee1234", 201);

        // when
        final var wrongPassword = login("gary@example.com", "wrong-password", 401);
        final var unknownEmail = login("nobody@example.com", "eevee1234", 401);

        // then
        assertThat(wrongPassword).isEqualTo(unknownEmail);
    }
}
