package com.challenge.aitools.taskmanagement.web.shared.security;

import org.springframework.security.oauth2.jwt.Jwt;

public final class AuthenticatedUser {

    private AuthenticatedUser() {
    }

    public static Long idOf(final Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
