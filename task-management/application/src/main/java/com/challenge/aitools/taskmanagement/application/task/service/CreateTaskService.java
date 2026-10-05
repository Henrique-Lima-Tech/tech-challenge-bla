package com.challenge.aitools.taskmanagement.application.task.service;

import com.challenge.aitools.taskmanagement.application.shared.port.out.Clock;
import com.challenge.aitools.taskmanagement.application.task.command.CreateTaskCommand;
import com.challenge.aitools.taskmanagement.application.task.port.in.CreateTask;
import com.challenge.aitools.taskmanagement.application.task.port.out.TaskRepository;
import com.challenge.aitools.taskmanagement.application.task.result.TaskResult;
import com.challenge.aitools.taskmanagement.domain.task.model.Task;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateTaskService implements CreateTask {

    private final TaskRepository taskRepository;
    private final Clock clock;

    @Override
    public TaskResult handle(final CreateTaskCommand command) {
        final var task = Task.create(command.title(), command.description(), command.status(), command.dueDate(),
                command.ownerId(), clock.now());
        return TaskResult.from(taskRepository.save(task));
    }
}
