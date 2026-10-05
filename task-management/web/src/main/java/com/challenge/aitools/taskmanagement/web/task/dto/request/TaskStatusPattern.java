package com.challenge.aitools.taskmanagement.web.task.dto.request;

/**
 * The status arrives as text and is checked here, so an unknown value comes back as a field error that
 * names the field instead of as an unreadable body.
 */
public final class TaskStatusPattern {

    public static final String REGEXP = "TODO|IN_PROGRESS|DONE";
    public static final String MESSAGE = "must be one of TODO, IN_PROGRESS, DONE";

    private TaskStatusPattern() {
    }
}
