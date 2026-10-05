package com.tech.challenge.domain.shared.exception;

/**
 * An external service the core depends on (for example the PokéAPI) could not answer.
 */
public class ExternalServiceUnavailableException extends RuntimeException {

    public ExternalServiceUnavailableException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
