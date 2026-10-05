package com.tech.challenge.infrastructure.user.security.password;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.tech.challenge.application.user.port.out.PasswordHasherPort;

@Component
public class BCryptPasswordHasher implements PasswordHasherPort {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String hash(final String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(final String rawPassword, final String passwordHash) {
        return encoder.matches(rawPassword, passwordHash);
    }
}
