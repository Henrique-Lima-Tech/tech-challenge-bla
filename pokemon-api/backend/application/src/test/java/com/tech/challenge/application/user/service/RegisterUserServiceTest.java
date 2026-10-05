package com.tech.challenge.application.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tech.challenge.application.user.command.RegisterUserCommand;
import com.tech.challenge.application.user.port.out.PasswordHasherPort;
import com.tech.challenge.application.user.port.out.UserRepositoryPort;
import com.tech.challenge.application.user.result.UserResult;
import com.tech.challenge.domain.user.exception.EmailAlreadyUsedException;
import com.tech.challenge.domain.user.model.User;

@ExtendWith(MockitoExtension.class)
class RegisterUserServiceTest {

    private static final String HASH = "$2a$10$hash";

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private PasswordHasherPort passwordHasherPort;

    @InjectMocks
    private RegisterUserService service;

    @Test
    void shouldSaveUserWithHashedPasswordWhenEmailIsNew() {
        // given
        when(userRepositoryPort.existsByEmail("ash@example.com")).thenReturn(false);
        when(passwordHasherPort.hash("pikachu123")).thenReturn(HASH);
        when(userRepositoryPort.save(new User(null, "Ash", "ash@example.com", HASH)))
                .thenReturn(new User(1L, "Ash", "ash@example.com", HASH));

        // when
        final var result = service.register(new RegisterUserCommand("Ash", "ash@example.com", "pikachu123"));

        // then
        assertThat(result).isEqualTo(new UserResult(1L, "Ash", "ash@example.com"));
    }

    @Test
    void shouldTrimAndLowercaseEmailWhenRegistering() {
        // given
        when(userRepositoryPort.existsByEmail("ash@example.com")).thenReturn(false);
        when(passwordHasherPort.hash("pikachu123")).thenReturn(HASH);
        when(userRepositoryPort.save(any())).thenReturn(new User(1L, "Ash", "ash@example.com", HASH));

        // when
        service.register(new RegisterUserCommand("Ash", " Ash@Example.COM ", "pikachu123"));

        // then
        verify(userRepositoryPort).existsByEmail("ash@example.com");
        verify(userRepositoryPort).save(new User(null, "Ash", "ash@example.com", HASH));
    }

    @Test
    void shouldThrowEmailAlreadyUsedWithoutHashingOrSavingWhenEmailExists() {
        // given
        when(userRepositoryPort.existsByEmail("ash@example.com")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> service.register(new RegisterUserCommand("Ash", "ash@example.com", "pikachu123")))
                .isInstanceOf(EmailAlreadyUsedException.class);

        verify(passwordHasherPort, never()).hash(anyString());
        verify(userRepositoryPort, never()).save(any());
    }

    @Test
    void shouldPropagateEmailAlreadyUsedWhenSaveDetectsDuplicate() {
        // given
        final var duplicate = new EmailAlreadyUsedException();
        when(userRepositoryPort.existsByEmail("ash@example.com")).thenReturn(false);
        when(passwordHasherPort.hash("pikachu123")).thenReturn(HASH);
        when(userRepositoryPort.save(any())).thenThrow(duplicate);

        // when & then
        assertThatThrownBy(() -> service.register(new RegisterUserCommand("Ash", "ash@example.com", "pikachu123")))
                .isSameAs(duplicate);
    }
}
