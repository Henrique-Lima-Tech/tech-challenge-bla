package com.tech.challenge.web.user.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tech.challenge.application.user.command.LoginCommand;
import com.tech.challenge.application.user.command.RegisterUserCommand;
import com.tech.challenge.application.user.port.in.LoginUseCase;
import com.tech.challenge.application.user.port.in.RegisterUserUseCase;
import com.tech.challenge.application.user.result.AccessTokenResult;
import com.tech.challenge.application.user.result.LoginResult;
import com.tech.challenge.application.user.result.UserResult;
import com.tech.challenge.domain.user.exception.EmailAlreadyUsedException;
import com.tech.challenge.domain.user.exception.InvalidCredentialsException;
import com.tech.challenge.web.shared.security.SecurityConfig;
import com.tech.challenge.web.user.mapper.AuthMapperImpl;

import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(AuthController.class)
@Import({ AuthMapperImpl.class, SecurityConfig.class })
class AuthControllerTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String PASSWORD = "pikachu123";
    private static final String EMAIL_255 = "a@" + ("b".repeat(60) + ".").repeat(4) + "c".repeat(9);

    private final MockMvc mockMvc;

    @MockitoBean
    private RegisterUserUseCase registerUserUseCase;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @Autowired
    AuthControllerTest(final MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    private static String registerBody(final String name, final String email, final String password) {
        final var body = new LinkedHashMap<String, String>();
        body.put("name", name);
        body.put("email", email);
        body.put("password", password);
        return JSON.writeValueAsString(body);
    }

    private static String loginBody(final String email, final String password) {
        final var body = new LinkedHashMap<String, String>();
        body.put("email", email);
        body.put("password", password);
        return JSON.writeValueAsString(body);
    }

    @Test
    void shouldReturnCreatedUserInContractShapeWhenRegistrationIsValid() throws Exception {
        // given
        when(registerUserUseCase.register(new RegisterUserCommand("Ash", "ash@example.com", PASSWORD)))
                .thenReturn(new UserResult(1L, "Ash", "ash@example.com"));

        // when & then
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("Ash", "ash@example.com", PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Ash"))
                .andExpect(jsonPath("$.email").value("ash@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(content().string(not(containsString(PASSWORD))));
    }

    @Test
    void shouldTrimNameAndEmailButNotPasswordWhenRegistering() throws Exception {
        // given
        when(registerUserUseCase.register(any())).thenReturn(new UserResult(1L, "Ash", "ash@example.com"));

        // when
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("  Ash ", " ash@example.com  ", " pikachu123 ")))
                .andExpect(status().isCreated());

        // then
        verify(registerUserUseCase).register(new RegisterUserCommand("Ash", "ash@example.com", " pikachu123 "));
    }

    static Stream<Arguments> validPasswords() {
        return Stream.of(
                Arguments.of("x".repeat(8)),
                Arguments.of("x".repeat(72)),
                Arguments.of("€".repeat(24)));
    }

    @ParameterizedTest
    @MethodSource("validPasswords")
    void shouldAcceptPasswordWhenWithinLimits(final String password) throws Exception {
        // given
        when(registerUserUseCase.register(any())).thenReturn(new UserResult(1L, "Ash", "ash@example.com"));

        // when & then
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("Ash", "ash@example.com", password)))
                .andExpect(status().isCreated());
    }

    static Stream<Arguments> invalidRegistrations() {
        return Stream.of(
                Arguments.of("name", registerBody(null, "ash@example.com", PASSWORD), "must not be blank"),
                Arguments.of("name", registerBody("   ", "ash@example.com", PASSWORD), "must not be blank"),
                Arguments.of("name", registerBody("a".repeat(101), "ash@example.com", PASSWORD), "size must be at most 100"),
                Arguments.of("email", registerBody("Ash", null, PASSWORD), "must not be blank"),
                Arguments.of("email", registerBody("Ash", "not-an-email", PASSWORD), "must be a well-formed email address"),
                Arguments.of("email", registerBody("Ash", EMAIL_255, PASSWORD), "size must be at most 254"),
                Arguments.of("password", registerBody("Ash", "ash@example.com", null), "must not be blank"),
                Arguments.of("password", registerBody("Ash", "ash@example.com", "x".repeat(7)), "size must be between 8 and 72"),
                Arguments.of("password", registerBody("Ash", "ash@example.com", "x".repeat(73)), "size must be between 8 and 72"),
                Arguments.of("password", registerBody("Ash", "ash@example.com", "€".repeat(25)), "size must be at most 72 bytes"));
    }

    @ParameterizedTest
    @MethodSource("invalidRegistrations")
    void shouldReturnBadRequestProblemWhenRegistrationIsInvalid(final String field, final String body,
            final String message) throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.instance").value("/api/v1/auth/register"))
                .andExpect(jsonPath("$.errors[?(@.field == '" + field + "')].message").value(hasItem(message)))
                .andExpect(content().string(not(containsString(PASSWORD))));

        verifyNoInteractions(registerUserUseCase);
    }

    @Test
    void shouldReturnBadRequestProblemWhenBodyIsMalformed() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Failed to read request"))
                .andExpect(content().string(not(containsString("Exception"))));

        verifyNoInteractions(registerUserUseCase);
    }

    @Test
    void shouldReturnBadRequestProblemWhenBodyIsEmpty() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Failed to read request"));

        verifyNoInteractions(loginUseCase);
    }

    @Test
    void shouldReturnConflictProblemWhenEmailIsAlreadyUsed() throws Exception {
        // given
        when(registerUserUseCase.register(any())).thenThrow(new EmailAlreadyUsedException());

        // when & then
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("Ash", "ash@example.com", PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Email already used"))
                .andExpect(jsonPath("$.instance").value("/api/v1/auth/register"));
    }

    @Test
    void shouldReturnTokenInContractShapeWhenCredentialsAreValid() throws Exception {
        // given
        when(loginUseCase.login(new LoginCommand("ash@example.com", PASSWORD)))
                .thenReturn(new LoginResult(new AccessTokenResult("signed.jwt.token", 3600), "Ash"));

        // when & then
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("ash@example.com", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value("signed.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.name").value("Ash"));
    }

    @Test
    void shouldIgnoreTokenWhenLoginRequestCarriesAnInvalidOne() throws Exception {
        // given
        when(loginUseCase.login(any())).thenReturn(new LoginResult(new AccessTokenResult("signed.jwt.token", 3600), "Ash"));

        // when & then
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer expired-or-invalid")
                        .content(loginBody("ash@example.com", PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldTrimEmailButNotPasswordWhenLoggingIn() throws Exception {
        // given
        when(loginUseCase.login(any())).thenReturn(new LoginResult(new AccessTokenResult("signed.jwt.token", 3600), "Ash"));

        // when
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(" ash@example.com ", " pikachu123 ")))
                .andExpect(status().isOk());

        // then
        verify(loginUseCase).login(new LoginCommand("ash@example.com", " pikachu123 "));
    }

    static Stream<Arguments> invalidLogins() {
        return Stream.of(
                Arguments.of("email", loginBody(null, PASSWORD), "must not be blank"),
                Arguments.of("email", loginBody("not-an-email", PASSWORD), "must be a well-formed email address"),
                Arguments.of("email", loginBody(EMAIL_255, PASSWORD), "size must be at most 254"),
                Arguments.of("password", loginBody("ash@example.com", null), "must not be blank"),
                Arguments.of("password", loginBody("ash@example.com", "  "), "must not be blank"));
    }

    @ParameterizedTest
    @MethodSource("invalidLogins")
    void shouldReturnBadRequestProblemWhenLoginIsInvalid(final String field, final String body, final String message)
            throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.instance").value("/api/v1/auth/login"))
                .andExpect(jsonPath("$.errors[?(@.field == '" + field + "')].message").value(hasItem(message)));

        verifyNoInteractions(loginUseCase);
    }

    @Test
    void shouldReturnGenericUnauthorizedProblemWhenCredentialsAreInvalid() throws Exception {
        // given
        when(loginUseCase.login(any())).thenThrow(new InvalidCredentialsException());

        // when & then
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("ash@example.com", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Invalid email or password"))
                .andExpect(jsonPath("$.instance").value("/api/v1/auth/login"))
                .andExpect(content().string(not(containsString("ash@example.com"))))
                .andExpect(content().string(not(containsString("Exception"))));
    }
}
