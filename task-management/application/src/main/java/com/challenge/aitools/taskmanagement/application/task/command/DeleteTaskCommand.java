package com.challenge.aitools.taskmanagement.application.task.command;

public record DeleteTaskCommand(Long ownerId, Long taskId) {
}
