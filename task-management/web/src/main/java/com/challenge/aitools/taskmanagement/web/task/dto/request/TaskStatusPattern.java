package com.challenge.aitools.taskmanagement.web.task.dto.request;

public final class TaskStatusPattern {

    public static final String REGEXP = "TODO|IN_PROGRESS|DONE";
    public static final String MESSAGE = "must be one of TODO, IN_PROGRESS, DONE";

    private TaskStatusPattern() {
    }
}
