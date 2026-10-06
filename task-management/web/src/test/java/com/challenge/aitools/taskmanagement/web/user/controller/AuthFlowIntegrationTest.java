package com.challenge.aitools.taskmanagement.web.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    private final MockMvc mockMvc;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Autowired
    AuthFlowIntegrationTest(final MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void shouldRegisterThenLoginThenReachAProtectedRouteWhenCredentialsAreValid() throws Exception {
        // given
        final var email = "flow-user@example.com";

        // when
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Flow User","email":"%s","password":"password123"}""".formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(email));

        final var token = login(email, "password123");

        // then
        assertThat(token).isNotBlank();
        mockMvc.perform(get("/api/v1/tasks").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void shouldRejectRegistrationWhenEmailIsAlreadyRegistered() throws Exception {
        // given
        final var email = "duplicate-user@example.com";
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"First","email":"%s","password":"password123"}""".formatted(email)))
                .andExpect(status().isCreated());

        // when & then
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Second","email":"%s","password":"password123"}""".formatted(email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Email already registered"));
    }

    @Test
    void shouldAcceptTheSameEmailInAnotherCaseWhenLoggingIn() throws Exception {
        // given
        final var email = "case-user@example.com";
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Case User","email":"%s","password":"password123"}""".formatted(email)))
                .andExpect(status().isCreated());

        // when & then
        assertThat(login("  CASE-USER@Example.COM  ", "password123")).isNotBlank();
    }

    @Test
    void shouldRejectLoginWhenPasswordIsWrong() throws Exception {
        // given
        final var email = "wrong-password-user@example.com";
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Wrong Password","email":"%s","password":"password123"}""".formatted(email)))
                .andExpect(status().isCreated());

        // when & then
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"not-the-password"}""".formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void shouldRejectLoginWhenEmailIsUnknown() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@example.com","password":"password123"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void shouldRejectAProtectedRouteWhenTokenIsExpiredOrInvalid() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/tasks").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Authentication required"));
    }

    private String login(final String email, final String password) throws Exception {
        final var response = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}""".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(response).get("accessToken").asString();
    }
}
