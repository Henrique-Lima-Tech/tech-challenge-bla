package com.challenge.aitools.taskmanagement.domain.task.exception;

import lombok.Getter;

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
