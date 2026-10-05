package com.challenge.aitools.taskmanagement.web.user.controller;

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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.challenge.aitools.taskmanagement.application.user.command.LoginCommand;
import com.challenge.aitools.taskmanagement.application.user.command.RegisterUserCommand;
import com.challenge.aitools.taskmanagement.application.user.port.in.Login;
import com.challenge.aitools.taskmanagement.application.user.port.in.RegisterUser;
import com.challenge.aitools.taskmanagement.application.user.result.AccessTokenResult;
import com.challenge.aitools.taskmanagement.application.user.result.UserResult;
import com.challenge.aitools.taskmanagement.domain.user.exception.EmailAlreadyRegisteredException;
import com.challenge.aitools.taskmanagement.domain.user.exception.InvalidCredentialsException;
import com.challenge.aitools.taskmanagement.web.shared.security.SecurityConfig;
import com.challenge.aitools.taskmanagement.web.user.mapper.AuthMapperImpl;

import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(AuthController.class)
@Import({ AuthMapperImpl.class, SecurityConfig.class })
class AuthControllerTest {

    private static final String REGISTER = "/api/v1/auth/register";
    private static final String LOGIN = "/api/v1/auth/login";
    private static final String PASSWORD = "password123";

    private final MockMvc mockMvc;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @MockitoBean
    private RegisterUser registerUser;

    @MockitoBean
    private Login login;

    @Autowired
    AuthControllerTest(final MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void shouldCreateUserWhenRegistrationIsValid() throws Exception {
        // given
        when(registerUser.handle(any(RegisterUserCommand.class)))
                .thenReturn(new UserResult(1L, "Demo User", "demo@example.com"));

        // when & then
        mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Demo User", "email", "demo@example.com", "password", PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Demo User"))
                .andExpect(jsonPath("$.email").value("demo@example.com"));
    }

    @Test
    void shouldNeverEchoThePasswordWhenRegistering() throws Exception {
        // given
        when(registerUser.handle(any(RegisterUserCommand.class)))
                .thenReturn(new UserResult(1L, "Demo User", "demo@example.com"));

        // when & then
        mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Demo User", "email", "demo@example.com", "password", PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(content().string(not(containsString(PASSWORD))));
    }

    @Test
    void shouldRejectRegistrationWhenNameIsBlank() throws Exception {
        // when & then
        mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "   ", "email", "demo@example.com", "password", PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value(REGISTER))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("name")))
                .andExpect(jsonPath("$.errors[*].message").value(hasItem("must not be blank")));
        verifyNoInteractions(registerUser);
    }

    @Test
    void shouldRejectRegistrationWhenEmailIsMalformed() throws Exception {
        // when & then
        mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Demo User", "email", "not-an-email", "password", PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("email")));
        verifyNoInteractions(registerUser);
    }

    @Test
    void shouldRejectRegistrationWhenPasswordIsTooShort() throws Exception {
        // when & then
        mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Demo User", "email", "demo@example.com", "password", "short12")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("password")))
                .andExpect(jsonPath("$.errors[*].message").value(hasItem("must be at least 8 characters")));
        verifyNoInteractions(registerUser);
    }

    @Test
    void shouldRejectRegistrationWhenPasswordExceedsSeventyTwoUtf8Bytes() throws Exception {
        // given
        final var password = "é".repeat(40);

        // when & then
        mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Demo User", "email", "demo@example.com", "password", password)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("password")))
                .andExpect(jsonPath("$.errors[*].message").value(hasItem("must be at most 72 bytes")));
        verifyNoInteractions(registerUser);
    }

    @Test
    void shouldRejectRegistrationWhenEmailIsAlreadyRegistered() throws Exception {
        // given
        when(registerUser.handle(any(RegisterUserCommand.class))).thenThrow(new EmailAlreadyRegisteredException());

        // when & then
        mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Demo User", "email", "demo@example.com", "password", PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Email already registered"));
    }

    @Test
    void shouldRejectRegistrationWhenBodyIsMalformed() throws Exception {
        // when & then
        mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON).content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Malformed request body"));
        verifyNoInteractions(registerUser);
    }

    @Test
    void shouldRejectRegistrationWhenBodyIsEmpty() throws Exception {
        // when & then
        mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON).content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Malformed request body"));
        verifyNoInteractions(registerUser);
    }

    @Test
    void shouldRejectRegistrationWhenAnUnknownFieldIsSent() throws Exception {
        // when & then
        mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Demo User", "email", "demo@example.com", "password", PASSWORD,
                                "id", "7")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("id"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be sent"));
        verifyNoInteractions(registerUser);
    }

    @Test
    void shouldReturnAccessTokenWhenLoginIsValid() throws Exception {
        // given
        when(login.handle(any(LoginCommand.class))).thenReturn(new AccessTokenResult("a.jwt.token"));

        // when & then
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "demo@example.com", "password", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("a.jwt.token"));
        verify(login).handle(new LoginCommand("demo@example.com", PASSWORD));
    }

    @Test
    void shouldReturnGenericUnauthorizedWhenCredentialsAreInvalid() throws Exception {
        // given
        when(login.handle(any(LoginCommand.class))).thenThrow(new InvalidCredentialsException());

        // when & then
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "demo@example.com", "password", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Invalid email or password"))
                .andExpect(content().string(not(containsString("wrong-password"))));
    }

    @Test
    void shouldRejectLoginWhenFieldsAreBlank() throws Exception {
        // when & then
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "", "password", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("email")))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("password")));
        verifyNoInteractions(login);
    }

    @Test
    void shouldNotRevealAMalformedEmailWhenLoggingIn() throws Exception {
        // given
        when(login.handle(any(LoginCommand.class))).thenThrow(new InvalidCredentialsException());

        // when & then
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "not-an-email", "password", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    private String json(final String... keysAndValues) {
        final var body = new LinkedHashMap<String, String>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            body.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return jsonMapper.writeValueAsString(body);
    }
}
