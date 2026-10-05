package com.challenge.aitools.taskmanagement.application.task.service;

import com.challenge.aitools.taskmanagement.application.shared.pagination.Page;
import com.challenge.aitools.taskmanagement.application.task.command.ListTasksCommand;
import com.challenge.aitools.taskmanagement.application.task.port.in.ListTasks;
import com.challenge.aitools.taskmanagement.application.task.port.out.TaskRepository;
import com.challenge.aitools.taskmanagement.application.task.result.TaskResult;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListTasksService implements ListTasks {

    private final TaskRepository taskRepository;

    @Override
    public Page<TaskResult> handle(final ListTasksCommand command) {
        return taskRepository.findByOwnerId(command.ownerId(), command.status(), command.page(), command.size())
                .map(TaskResult::from);
    }
}
