package com.challenge.aitools.taskmanagement.web.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.challenge.aitools.taskmanagement.application.user.port.in.Login;
import com.challenge.aitools.taskmanagement.application.user.port.in.RegisterUser;
import com.challenge.aitools.taskmanagement.application.user.port.out.PasswordHasher;
import com.challenge.aitools.taskmanagement.application.user.port.out.TokenIssuer;
import com.challenge.aitools.taskmanagement.application.user.port.out.UserRepository;
import com.challenge.aitools.taskmanagement.application.user.service.LoginService;
import com.challenge.aitools.taskmanagement.application.user.service.RegisterUserService;

@Configuration
public class UserUseCaseConfig {

    @Bean
    RegisterUser registerUser(final UserRepository userRepository, final PasswordHasher passwordHasher) {
        return new RegisterUserService(userRepository, passwordHasher);
    }

    @Bean
    Login login(final UserRepository userRepository, final PasswordHasher passwordHasher,
            final TokenIssuer tokenIssuer) {
        return new LoginService(userRepository, passwordHasher, tokenIssuer);
    }
}
