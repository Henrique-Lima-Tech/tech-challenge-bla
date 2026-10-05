package com.challenge.aitools.taskmanagement.domain.task.exception;

import lombok.Getter;

/**
 * A broken task invariant, carrying the field it is about so the API can answer with the same
 * {@code field}/{@code message} shape as a Bean Validation error.
 */
@Getter
public class InvalidTaskException extends RuntimeException {

    private final String field;
    private final String reason;

    public InvalidTaskException(final String field, final String reason) {
        super(field + " " + reason);
        this.field = field;
        this.reason = reason;
    }
}
