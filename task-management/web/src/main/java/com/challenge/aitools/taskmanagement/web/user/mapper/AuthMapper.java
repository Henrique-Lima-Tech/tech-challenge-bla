package com.challenge.aitools.taskmanagement.web.user.mapper;

import org.mapstruct.Mapper;

import com.challenge.aitools.taskmanagement.application.user.command.LoginCommand;
import com.challenge.aitools.taskmanagement.application.user.command.RegisterUserCommand;
import com.challenge.aitools.taskmanagement.application.user.result.AccessTokenResult;
import com.challenge.aitools.taskmanagement.application.user.result.UserResult;
import com.challenge.aitools.taskmanagement.web.user.dto.request.LoginRequest;
import com.challenge.aitools.taskmanagement.web.user.dto.request.RegisterRequest;
import com.challenge.aitools.taskmanagement.web.user.dto.response.TokenResponse;
import com.challenge.aitools.taskmanagement.web.user.dto.response.UserResponse;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    RegisterUserCommand toCommand(RegisterRequest request);

    LoginCommand toCommand(LoginRequest request);

    UserResponse toResponse(UserResult result);

    TokenResponse toResponse(AccessTokenResult result);
}
