package com.challenge.aitools.taskmanagement.application.user.port.in;

import com.challenge.aitools.taskmanagement.application.user.command.LoginCommand;
import com.challenge.aitools.taskmanagement.application.user.result.AccessTokenResult;

public interface Login {

    AccessTokenResult handle(LoginCommand command);
}
