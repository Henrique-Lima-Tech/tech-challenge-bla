package com.challenge.aitools.taskmanagement.application.task.service;

import com.challenge.aitools.taskmanagement.application.task.command.DeleteTaskCommand;
import com.challenge.aitools.taskmanagement.application.task.port.in.DeleteTask;
import com.challenge.aitools.taskmanagement.application.task.port.out.TaskRepository;
import com.challenge.aitools.taskmanagement.domain.task.exception.TaskNotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeleteTaskService implements DeleteTask {

    private final TaskRepository taskRepository;

    @Override
    public void handle(final DeleteTaskCommand command) {
        final var task = taskRepository.findByIdAndOwnerId(command.taskId(), command.ownerId())
                .orElseThrow(TaskNotFoundException::new);
        taskRepository.delete(task);
    }
}
