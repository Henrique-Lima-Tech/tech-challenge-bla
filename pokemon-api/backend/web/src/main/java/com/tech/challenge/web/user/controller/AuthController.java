package com.tech.challenge.web.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tech.challenge.application.user.port.in.LoginUseCase;
import com.tech.challenge.application.user.port.in.RegisterUserUseCase;
import com.tech.challenge.web.user.dto.request.LoginRequest;
import com.tech.challenge.web.user.dto.request.RegisterRequest;
import com.tech.challenge.web.user.dto.response.TokenResponse;
import com.tech.challenge.web.user.dto.response.UserResponse;
import com.tech.challenge.web.user.mapper.AuthMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final AuthMapper authMapper;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@RequestBody @Valid final RegisterRequest request) {
        log.debug("Received registration request");
        final var user = registerUserUseCase.register(authMapper.toCommand(request));
        log.debug("Registered user {}", user.id());
        return authMapper.toResponse(user);
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody @Valid final LoginRequest request) {
        log.debug("Received login request");
        return authMapper.toResponse(loginUseCase.login(authMapper.toCommand(request)));
    }
}
