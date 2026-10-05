package com.challenge.aitools.taskmanagement.application.task.service;

import com.challenge.aitools.taskmanagement.application.task.command.GetTaskCommand;
import com.challenge.aitools.taskmanagement.application.task.port.in.GetTask;
import com.challenge.aitools.taskmanagement.application.task.port.out.TaskRepository;
import com.challenge.aitools.taskmanagement.application.task.result.TaskResult;
import com.challenge.aitools.taskmanagement.domain.task.exception.TaskNotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetTaskService implements GetTask {

    private final TaskRepository taskRepository;

    @Override
    public TaskResult handle(final GetTaskCommand command) {
        return taskRepository.findByIdAndOwnerId(command.taskId(), command.ownerId())
                .map(TaskResult::from)
                .orElseThrow(TaskNotFoundException::new);
    }
}
