package com.challenge.aitools.taskmanagement.application.task.command;

import com.challenge.aitools.taskmanagement.domain.task.model.TaskStatus;

public record ListTasksCommand(Long ownerId, TaskStatus status, int page, int size) {
}
