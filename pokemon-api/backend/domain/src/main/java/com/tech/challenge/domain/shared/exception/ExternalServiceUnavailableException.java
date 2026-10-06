package com.tech.challenge.domain.shared.exception;

public class ExternalServiceUnavailableException extends RuntimeException {

    public ExternalServiceUnavailableException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
