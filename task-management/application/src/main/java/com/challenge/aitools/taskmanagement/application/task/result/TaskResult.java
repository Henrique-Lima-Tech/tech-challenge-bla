package com.challenge.aitools.taskmanagement.application.task.result;

import java.time.Instant;
import java.time.LocalDate;

import com.challenge.aitools.taskmanagement.domain.task.model.Task;
import com.challenge.aitools.taskmanagement.domain.task.model.TaskStatus;

public record TaskResult(Long id, String title, String description, TaskStatus status, LocalDate dueDate,
        Instant createdAt, Instant updatedAt) {

    public static TaskResult from(final Task task) {
        return new TaskResult(task.id(), task.title(), task.description(), task.status(), task.dueDate(),
                task.createdAt(), task.updatedAt());
    }
}
