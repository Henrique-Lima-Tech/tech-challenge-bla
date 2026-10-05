package com.challenge.aitools.taskmanagement.application.task.port.in;

import com.challenge.aitools.taskmanagement.application.task.command.DeleteTaskCommand;

public interface DeleteTask {

    void handle(DeleteTaskCommand command);
}
