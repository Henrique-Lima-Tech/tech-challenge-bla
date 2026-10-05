package com.tech.challenge.application.user.port.out;

public interface PasswordHasherPort {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}
