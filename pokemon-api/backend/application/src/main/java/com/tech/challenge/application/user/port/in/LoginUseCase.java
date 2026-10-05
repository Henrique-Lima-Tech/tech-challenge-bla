package com.tech.challenge.application.user.port.in;

import com.tech.challenge.application.user.command.LoginCommand;
import com.tech.challenge.application.user.result.LoginResult;

/**
 * REQ-API02: authentication.
 */
public interface LoginUseCase {

    /**
     * @throws com.tech.challenge.domain.user.exception.InvalidCredentialsException if the email is unknown or the
     *         password is wrong
     */
    LoginResult login(LoginCommand command);
}
