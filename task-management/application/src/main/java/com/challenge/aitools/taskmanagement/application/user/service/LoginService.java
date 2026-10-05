package com.challenge.aitools.taskmanagement.application.user.service;

import com.challenge.aitools.taskmanagement.application.user.command.LoginCommand;
import com.challenge.aitools.taskmanagement.application.user.port.in.Login;
import com.challenge.aitools.taskmanagement.application.user.port.out.PasswordHasher;
import com.challenge.aitools.taskmanagement.application.user.port.out.TokenIssuer;
import com.challenge.aitools.taskmanagement.application.user.port.out.UserRepository;
import com.challenge.aitools.taskmanagement.application.user.result.AccessTokenResult;
import com.challenge.aitools.taskmanagement.domain.user.exception.InvalidCredentialsException;
import com.challenge.aitools.taskmanagement.domain.user.model.User;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LoginService implements Login {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;

    @Override
    public AccessTokenResult handle(final LoginCommand command) {
        final var email = User.normalizeEmail(command.email());
        final var user = userRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        if (!passwordHasher.matches(command.password(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return new AccessTokenResult(tokenIssuer.issue(user));
    }
}
