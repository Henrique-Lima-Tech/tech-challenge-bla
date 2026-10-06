package com.challenge.aitools.taskmanagement.domain.user.model;

import java.util.Locale;

import com.challenge.aitools.taskmanagement.domain.user.exception.InvalidUserException;

public record User(Long id, String name, String email, String passwordHash) {

    public static final int NAME_MAX_LENGTH = 100;
    public static final int EMAIL_MAX_LENGTH = 254;

    public User {
        if (id != null && id <= 0) {
            throw new InvalidUserException("id", "must be positive");
        }
        if (name == null || name.isBlank()) {
            throw new InvalidUserException("name", "must not be blank");
        }
        if (email == null || email.isBlank()) {
            throw new InvalidUserException("email", "must not be blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new InvalidUserException("passwordHash", "must not be blank");
        }
        name = name.trim();
        email = normalizeEmail(email);
        if (name.length() > NAME_MAX_LENGTH) {
            throw new InvalidUserException("name", "must be at most " + NAME_MAX_LENGTH + " characters");
        }
        if (email.length() > EMAIL_MAX_LENGTH) {
            throw new InvalidUserException("email", "must be at most " + EMAIL_MAX_LENGTH + " characters");
        }
    }

    public static String normalizeEmail(final String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "User[id=" + id + "]";
    }
}
