package com.challenge.aitools.taskmanagement.application.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.challenge.aitools.taskmanagement.application.user.command.RegisterUserCommand;
import com.challenge.aitools.taskmanagement.application.user.port.out.PasswordHasher;
import com.challenge.aitools.taskmanagement.application.user.port.out.UserRepository;
import com.challenge.aitools.taskmanagement.domain.user.exception.EmailAlreadyRegisteredException;
import com.challenge.aitools.taskmanagement.domain.user.model.User;

@ExtendWith(MockitoExtension.class)
class RegisterUserServiceTest {

    private static final String HASH = "$2a$10$hash";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @InjectMocks
    private RegisterUserService registerUserService;

    @Test
    void shouldRegisterUserWhenEmailIsFree() {
        // given
        final var command = new RegisterUserCommand("Demo User", "demo@example.com", "password123");
        when(userRepository.existsByEmail("demo@example.com")).thenReturn(false);
        when(passwordHasher.hash("password123")).thenReturn(HASH);
        when(userRepository.save(any(User.class)))
                .thenReturn(new User(1L, "Demo User", "demo@example.com", HASH));

        // when
        final var result = registerUserService.handle(command);

        // then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Demo User");
        assertThat(result.email()).isEqualTo("demo@example.com");
    }

    @Test
    void shouldStoreOnlyTheHashedPasswordWhenRegistering() {
        // given
        final var command = new RegisterUserCommand("Demo User", "demo@example.com", "password123");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordHasher.hash("password123")).thenReturn(HASH);
        when(userRepository.save(any(User.class)))
                .thenReturn(new User(1L, "Demo User", "demo@example.com", HASH));

        // when
        registerUserService.handle(command);

        // then
        final var saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().passwordHash()).isEqualTo(HASH);
        assertThat(saved.getValue().passwordHash()).isNotEqualTo("password123");
    }

    @Test
    void shouldNormalizeEmailWhenRegistering() {
        // given
        final var command = new RegisterUserCommand("Demo User", "  DEMO@Example.COM  ", "password123");
        when(userRepository.existsByEmail("demo@example.com")).thenReturn(false);
        when(passwordHasher.hash(anyString())).thenReturn(HASH);
        when(userRepository.save(any(User.class)))
                .thenReturn(new User(1L, "Demo User", "demo@example.com", HASH));

        // when
        registerUserService.handle(command);

        // then
        final var saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().email()).isEqualTo("demo@example.com");
        verify(userRepository).existsByEmail("demo@example.com");
    }

    @Test
    void shouldRejectRegistrationWhenEmailIsAlreadyRegistered() {
        // given
        final var command = new RegisterUserCommand("Demo User", "DEMO@example.com", "password123");
        when(userRepository.existsByEmail("demo@example.com")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> registerUserService.handle(command))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
        verify(userRepository, never()).save(any(User.class));
    }
}
