package com.challenge.aitools.taskmanagement.application.user.port.in;

import com.challenge.aitools.taskmanagement.application.user.command.RegisterUserCommand;
import com.challenge.aitools.taskmanagement.application.user.result.UserResult;

public interface RegisterUser {

    UserResult handle(RegisterUserCommand command);
}
