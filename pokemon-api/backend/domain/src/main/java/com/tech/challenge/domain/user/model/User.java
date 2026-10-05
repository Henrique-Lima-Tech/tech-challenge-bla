package com.tech.challenge.domain.user.model;

/**
 * A registered user (REQ-API02). {@code id} is {@code null} until the user is saved.
 */
public record User(Long id, String name, String email, String passwordHash) {

    public User {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("User id must be positive");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("User name must not be blank");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("User email must not be blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("User password hash must not be blank");
        }
    }

    @Override
    public String toString() {
        return "User[id=" + id + "]";
    }
}
