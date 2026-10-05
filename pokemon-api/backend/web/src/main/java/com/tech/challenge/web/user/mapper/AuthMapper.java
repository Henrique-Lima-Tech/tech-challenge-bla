package com.tech.challenge.web.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.tech.challenge.application.user.command.LoginCommand;
import com.tech.challenge.application.user.command.RegisterUserCommand;
import com.tech.challenge.application.user.result.LoginResult;
import com.tech.challenge.application.user.result.UserResult;
import com.tech.challenge.web.user.dto.request.LoginRequest;
import com.tech.challenge.web.user.dto.request.RegisterRequest;
import com.tech.challenge.web.user.dto.response.TokenResponse;
import com.tech.challenge.web.user.dto.response.UserResponse;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    RegisterUserCommand toCommand(RegisterRequest request);

    LoginCommand toCommand(LoginRequest request);

    UserResponse toResponse(UserResult result);

    @Mapping(target = "accessToken", source = "accessToken.value")
    @Mapping(target = "tokenType", constant = "Bearer")
    @Mapping(target = "expiresIn", source = "accessToken.expiresInSeconds")
    TokenResponse toResponse(LoginResult result);
}
