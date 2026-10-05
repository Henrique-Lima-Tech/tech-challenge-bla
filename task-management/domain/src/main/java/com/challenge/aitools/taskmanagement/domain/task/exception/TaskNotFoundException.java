package com.challenge.aitools.taskmanagement.domain.task.exception;

public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException() {
        super("Task not found");
    }
}
