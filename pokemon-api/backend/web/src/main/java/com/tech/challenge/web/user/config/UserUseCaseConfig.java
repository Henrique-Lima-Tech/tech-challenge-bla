package com.tech.challenge.web.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tech.challenge.application.user.port.in.LoginUseCase;
import com.tech.challenge.application.user.port.in.RegisterUserUseCase;
import com.tech.challenge.application.user.port.out.PasswordHasherPort;
import com.tech.challenge.application.user.port.out.TokenPort;
import com.tech.challenge.application.user.port.out.UserRepositoryPort;
import com.tech.challenge.application.user.service.LoginService;
import com.tech.challenge.application.user.service.RegisterUserService;

/**
 * Registers the framework-free user services as Spring beans.
 */
@Configuration
public class UserUseCaseConfig {

    @Bean
    RegisterUserUseCase registerUserUseCase(final UserRepositoryPort userRepositoryPort,
            final PasswordHasherPort passwordHasherPort) {
        return new RegisterUserService(userRepositoryPort, passwordHasherPort);
    }

    @Bean
    LoginUseCase loginUseCase(final UserRepositoryPort userRepositoryPort, final PasswordHasherPort passwordHasherPort,
            final TokenPort tokenPort) {
        return new LoginService(userRepositoryPort, passwordHasherPort, tokenPort);
    }
}
