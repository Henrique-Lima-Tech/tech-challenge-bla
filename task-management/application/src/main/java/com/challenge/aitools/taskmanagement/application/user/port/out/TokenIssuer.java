package com.challenge.aitools.taskmanagement.application.user.port.out;

import com.challenge.aitools.taskmanagement.domain.user.model.User;

public interface TokenIssuer {

    String issue(User user);
}
