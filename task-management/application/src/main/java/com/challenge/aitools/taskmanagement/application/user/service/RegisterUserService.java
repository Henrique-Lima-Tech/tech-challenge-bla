package com.challenge.aitools.taskmanagement.application.user.service;

import com.challenge.aitools.taskmanagement.application.user.command.RegisterUserCommand;
import com.challenge.aitools.taskmanagement.application.user.port.in.RegisterUser;
import com.challenge.aitools.taskmanagement.application.user.port.out.PasswordHasher;
import com.challenge.aitools.taskmanagement.application.user.port.out.UserRepository;
import com.challenge.aitools.taskmanagement.application.user.result.UserResult;
import com.challenge.aitools.taskmanagement.domain.user.exception.EmailAlreadyRegisteredException;
import com.challenge.aitools.taskmanagement.domain.user.model.User;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RegisterUserService implements RegisterUser {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    @Override
    public UserResult handle(final RegisterUserCommand command) {
        final var email = User.normalizeEmail(command.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        final var user = new User(null, command.name(), email, passwordHasher.hash(command.password()));
        final var saved = userRepository.save(user);
        return new UserResult(saved.id(), saved.name(), saved.email());
    }
}
