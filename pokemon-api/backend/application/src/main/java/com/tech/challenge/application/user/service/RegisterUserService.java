package com.tech.challenge.application.user.service;

import java.util.Locale;

import com.tech.challenge.application.user.command.RegisterUserCommand;
import com.tech.challenge.application.user.port.in.RegisterUserUseCase;
import com.tech.challenge.application.user.port.out.PasswordHasherPort;
import com.tech.challenge.application.user.port.out.UserRepositoryPort;
import com.tech.challenge.application.user.result.UserResult;
import com.tech.challenge.domain.user.exception.EmailAlreadyUsedException;
import com.tech.challenge.domain.user.model.User;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RegisterUserService implements RegisterUserUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;

    @Override
    public UserResult register(final RegisterUserCommand command) {
        final var email = command.email().trim().toLowerCase(Locale.ROOT);
        if (userRepositoryPort.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }
        final var user = new User(null, command.name(), email, passwordHasherPort.hash(command.password()));
        final var saved = userRepositoryPort.save(user);
        return new UserResult(saved.id(), saved.name(), saved.email());
    }
}
