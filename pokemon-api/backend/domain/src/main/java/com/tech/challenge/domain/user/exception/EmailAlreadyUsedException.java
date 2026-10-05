package com.tech.challenge.domain.user.exception;

public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException() {
        super("Email already used");
    }
}
