package com.tech.challenge.application.user.port.in;

import com.tech.challenge.application.user.command.RegisterUserCommand;
import com.tech.challenge.application.user.result.UserResult;

public interface RegisterUserUseCase {

    UserResult register(RegisterUserCommand command);
}
