package com.tech.challenge.application.user.port.in;

import com.tech.challenge.application.user.command.RegisterUserCommand;
import com.tech.challenge.application.user.result.UserResult;

/**
 * REQ-API02: user registration.
 */
public interface RegisterUserUseCase {

    /**
     * @throws com.tech.challenge.domain.user.exception.EmailAlreadyUsedException if the email is already registered
     */
    UserResult register(RegisterUserCommand command);
}
