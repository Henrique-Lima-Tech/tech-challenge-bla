package com.challenge.aitools.taskmanagement.application.task.port.in;

import com.challenge.aitools.taskmanagement.application.task.command.GetTaskCommand;
import com.challenge.aitools.taskmanagement.application.task.result.TaskResult;

public interface GetTask {

    TaskResult handle(GetTaskCommand command);
}
