package com.challenge.aitools.taskmanagement.application.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.challenge.aitools.taskmanagement.application.user.command.LoginCommand;
import com.challenge.aitools.taskmanagement.application.user.port.out.PasswordHasher;
import com.challenge.aitools.taskmanagement.application.user.port.out.TokenIssuer;
import com.challenge.aitools.taskmanagement.application.user.port.out.UserRepository;
import com.challenge.aitools.taskmanagement.domain.user.exception.InvalidCredentialsException;
import com.challenge.aitools.taskmanagement.domain.user.model.User;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    private static final String HASH = "$2a$10$hash";
    private static final User USER = new User(1L, "Demo User", "demo@example.com", HASH);

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenIssuer tokenIssuer;

    @InjectMocks
    private LoginService loginService;

    @Test
    void shouldReturnAccessTokenWhenCredentialsAreValid() {
        // given
        final var command = new LoginCommand("demo@example.com", "password123");
        when(userRepository.findByEmail("demo@example.com")).thenReturn(Optional.of(USER));
        when(passwordHasher.matches("password123", HASH)).thenReturn(true);
        when(tokenIssuer.issue(USER)).thenReturn("a.jwt.token");

        // when
        final var result = loginService.handle(command);

        // then
        assertThat(result.accessToken()).isEqualTo("a.jwt.token");
    }

    @Test
    void shouldNormalizeEmailWhenLoggingIn() {
        // given
        final var command = new LoginCommand("  DEMO@Example.COM  ", "password123");
        when(userRepository.findByEmail("demo@example.com")).thenReturn(Optional.of(USER));
        when(passwordHasher.matches(anyString(), anyString())).thenReturn(true);
        when(tokenIssuer.issue(USER)).thenReturn("a.jwt.token");

        // when
        loginService.handle(command);

        // then
        verify(userRepository).findByEmail("demo@example.com");
    }

    @Test
    void shouldRejectLoginWhenEmailIsUnknown() {
        // given
        final var command = new LoginCommand("missing@example.com", "password123");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> loginService.handle(command))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(tokenIssuer, never()).issue(any(User.class));
    }

    @Test
    void shouldRejectLoginWhenPasswordDoesNotMatch() {
        // given
        final var command = new LoginCommand("demo@example.com", "wrong-password");
        when(userRepository.findByEmail("demo@example.com")).thenReturn(Optional.of(USER));
        when(passwordHasher.matches("wrong-password", HASH)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> loginService.handle(command))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(tokenIssuer, never()).issue(any(User.class));
    }
}
