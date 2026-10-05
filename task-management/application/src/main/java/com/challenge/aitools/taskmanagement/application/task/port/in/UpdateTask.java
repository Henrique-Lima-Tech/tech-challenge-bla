package com.challenge.aitools.taskmanagement.application.task.port.in;

import com.challenge.aitools.taskmanagement.application.task.command.UpdateTaskCommand;
import com.challenge.aitools.taskmanagement.application.task.result.TaskResult;

public interface UpdateTask {

    TaskResult handle(UpdateTaskCommand command);
}
