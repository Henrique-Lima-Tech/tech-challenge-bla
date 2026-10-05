package com.tech.challenge.application.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tech.challenge.application.user.command.LoginCommand;
import com.tech.challenge.application.user.port.out.PasswordHasherPort;
import com.tech.challenge.application.user.port.out.TokenPort;
import com.tech.challenge.application.user.port.out.UserRepositoryPort;
import com.tech.challenge.application.user.result.AccessTokenResult;
import com.tech.challenge.application.user.result.LoginResult;
import com.tech.challenge.domain.user.exception.InvalidCredentialsException;
import com.tech.challenge.domain.user.model.User;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    private static final String HASH = "$2a$10$hash";
    private static final User ASH = new User(1L, "Ash", "ash@example.com", HASH);
    private static final AccessTokenResult TOKEN = new AccessTokenResult("signed.jwt.token", 3600);

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private PasswordHasherPort passwordHasherPort;

    @Mock
    private TokenPort tokenPort;

    @InjectMocks
    private LoginService service;

    @Test
    void shouldIssueTokenWithTheUsersNameWhenCredentialsAreValid() {
        // given
        when(userRepositoryPort.findByEmail("ash@example.com")).thenReturn(Optional.of(ASH));
        when(passwordHasherPort.matches("pikachu123", HASH)).thenReturn(true);
        when(tokenPort.issue(ASH)).thenReturn(TOKEN);

        // when
        final var result = service.login(new LoginCommand("ash@example.com", "pikachu123"));

        // then
        assertThat(result).isEqualTo(new LoginResult(TOKEN, "Ash"));
    }

    @Test
    void shouldTrimAndLowercaseEmailWhenLoggingIn() {
        // given
        when(userRepositoryPort.findByEmail("ash@example.com")).thenReturn(Optional.of(ASH));
        when(passwordHasherPort.matches("pikachu123", HASH)).thenReturn(true);
        when(tokenPort.issue(ASH)).thenReturn(TOKEN);

        // when
        service.login(new LoginCommand(" Ash@Example.COM ", "pikachu123"));

        // then
        verify(userRepositoryPort).findByEmail("ash@example.com");
    }

    @Test
    void shouldThrowInvalidCredentialsWhenEmailIsUnknown() {
        // given
        when(userRepositoryPort.findByEmail("misty@example.com")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.login(new LoginCommand("misty@example.com", "pikachu123")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(tokenPort, never()).issue(any());
    }

    @Test
    void shouldThrowInvalidCredentialsWhenPasswordIsWrong() {
        // given
        when(userRepositoryPort.findByEmail("ash@example.com")).thenReturn(Optional.of(ASH));
        when(passwordHasherPort.matches("wrong-password", HASH)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> service.login(new LoginCommand("ash@example.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(tokenPort, never()).issue(any());
    }

    @Test
    void shouldNotTrimPasswordWhenLoggingIn() {
        // given
        when(userRepositoryPort.findByEmail("ash@example.com")).thenReturn(Optional.of(ASH));
        when(passwordHasherPort.matches(" pikachu123 ", HASH)).thenReturn(true);
        when(tokenPort.issue(ASH)).thenReturn(TOKEN);

        // when
        service.login(new LoginCommand("ash@example.com", " pikachu123 "));

        // then
        verify(passwordHasherPort).matches(" pikachu123 ", HASH);
    }
}
