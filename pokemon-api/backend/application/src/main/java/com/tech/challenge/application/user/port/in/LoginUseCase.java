package com.tech.challenge.application.user.port.in;

import com.tech.challenge.application.user.command.LoginCommand;
import com.tech.challenge.application.user.result.LoginResult;

public interface LoginUseCase {

    LoginResult login(LoginCommand command);
}
