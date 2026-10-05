package com.challenge.aitools.taskmanagement.web.shared.security;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * The owner of every task operation comes from the token's subject, never from the request.
 */
public final class AuthenticatedUser {

    private AuthenticatedUser() {
    }

    public static Long idOf(final Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
