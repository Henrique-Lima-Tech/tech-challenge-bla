package com.challenge.aitools.taskmanagement.application.task.port.in;

import com.challenge.aitools.taskmanagement.application.task.command.CreateTaskCommand;
import com.challenge.aitools.taskmanagement.application.task.result.TaskResult;

public interface CreateTask {

    TaskResult handle(CreateTaskCommand command);
}
