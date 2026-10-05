package com.challenge.aitools.taskmanagement.application.task.command;

import java.time.LocalDate;

import com.challenge.aitools.taskmanagement.domain.task.model.TaskStatus;

public record UpdateTaskCommand(Long ownerId, Long taskId, String title, String description, TaskStatus status,
        LocalDate dueDate) {
}
