package com.challenge.aitools.taskmanagement.web.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.challenge.aitools.taskmanagement.application.user.port.in.Login;
import com.challenge.aitools.taskmanagement.application.user.port.in.RegisterUser;
import com.challenge.aitools.taskmanagement.web.user.dto.request.LoginRequest;
import com.challenge.aitools.taskmanagement.web.user.dto.request.RegisterRequest;
import com.challenge.aitools.taskmanagement.web.user.dto.response.TokenResponse;
import com.challenge.aitools.taskmanagement.web.user.dto.response.UserResponse;
import com.challenge.aitools.taskmanagement.web.user.mapper.AuthMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUser registerUser;
    private final Login login;
    private final AuthMapper authMapper;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse register(@Valid @RequestBody final RegisterRequest request) {
        log.debug("Registering a new user");
        final var result = registerUser.handle(authMapper.toCommand(request));
        log.debug("Registered user {}", result.id());
        return authMapper.toResponse(result);
    }

    @PostMapping("/login")
    TokenResponse login(@Valid @RequestBody final LoginRequest request) {
        log.debug("Authenticating a user");
        return authMapper.toResponse(login.handle(authMapper.toCommand(request)));
    }
}
