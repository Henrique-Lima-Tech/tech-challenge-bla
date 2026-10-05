package com.challenge.aitools.taskmanagement.application.task.port.in;

import com.challenge.aitools.taskmanagement.application.shared.pagination.Page;
import com.challenge.aitools.taskmanagement.application.task.command.ListTasksCommand;
import com.challenge.aitools.taskmanagement.application.task.result.TaskResult;

public interface ListTasks {

    Page<TaskResult> handle(ListTasksCommand command);
}
