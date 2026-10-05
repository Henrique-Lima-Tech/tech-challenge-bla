package com.tech.challenge.application.user.service;

import java.util.Locale;

import com.tech.challenge.application.user.command.LoginCommand;
import com.tech.challenge.application.user.port.in.LoginUseCase;
import com.tech.challenge.application.user.port.out.PasswordHasherPort;
import com.tech.challenge.application.user.port.out.TokenPort;
import com.tech.challenge.application.user.port.out.UserRepositoryPort;
import com.tech.challenge.application.user.result.LoginResult;
import com.tech.challenge.domain.user.exception.InvalidCredentialsException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LoginService implements LoginUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final TokenPort tokenPort;

    @Override
    public LoginResult login(final LoginCommand command) {
        final var email = command.email().trim().toLowerCase(Locale.ROOT);
        return userRepositoryPort.findByEmail(email)
                .filter(user -> passwordHasherPort.matches(command.password(), user.passwordHash()))
                .map(user -> new LoginResult(tokenPort.issue(user), user.name()))
                .orElseThrow(InvalidCredentialsException::new);
    }
}
