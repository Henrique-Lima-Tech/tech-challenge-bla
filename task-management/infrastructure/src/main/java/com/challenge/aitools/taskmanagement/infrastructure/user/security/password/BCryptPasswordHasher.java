package com.challenge.aitools.taskmanagement.infrastructure.user.security.password;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.challenge.aitools.taskmanagement.application.user.port.out.PasswordHasher;

@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public String hash(final String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(final String rawPassword, final String passwordHash) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }
}
