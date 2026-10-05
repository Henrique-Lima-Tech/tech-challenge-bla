package com.tech.challenge.domain.user.exception;

/**
 * Unknown email or wrong password: deliberately the same exception for both.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid credentials");
    }
}
