package com.challenge.aitools.taskmanagement.application.task.service;

import com.challenge.aitools.taskmanagement.application.shared.port.out.Clock;
import com.challenge.aitools.taskmanagement.application.task.command.UpdateTaskCommand;
import com.challenge.aitools.taskmanagement.application.task.port.in.UpdateTask;
import com.challenge.aitools.taskmanagement.application.task.port.out.TaskRepository;
import com.challenge.aitools.taskmanagement.application.task.result.TaskResult;
import com.challenge.aitools.taskmanagement.domain.task.exception.TaskNotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateTaskService implements UpdateTask {

    private final TaskRepository taskRepository;
    private final Clock clock;

    @Override
    public TaskResult handle(final UpdateTaskCommand command) {
        final var task = taskRepository.findByIdAndOwnerId(command.taskId(), command.ownerId())
                .orElseThrow(TaskNotFoundException::new);
        final var updated = task.replace(command.title(), command.description(), command.status(), command.dueDate(),
                clock.now());
        return TaskResult.from(taskRepository.save(updated));
    }
}
