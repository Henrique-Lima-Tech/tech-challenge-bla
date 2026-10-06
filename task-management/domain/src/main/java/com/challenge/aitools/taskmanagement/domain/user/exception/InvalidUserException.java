package com.challenge.aitools.taskmanagement.domain.user.exception;

import lombok.Getter;

@Getter
public class InvalidUserException extends RuntimeException {

    private final String field;
    private final String reason;

    public InvalidUserException(final String field, final String reason) {
        super(field + " " + reason);
        this.field = field;
        this.reason = reason;
    }
}
