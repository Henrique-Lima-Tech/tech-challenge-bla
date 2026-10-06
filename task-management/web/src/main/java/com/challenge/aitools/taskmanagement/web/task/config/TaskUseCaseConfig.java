package com.challenge.aitools.taskmanagement.web.task.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.challenge.aitools.taskmanagement.application.shared.port.out.Clock;
import com.challenge.aitools.taskmanagement.application.task.port.in.CreateTask;
import com.challenge.aitools.taskmanagement.application.task.port.in.DeleteTask;
import com.challenge.aitools.taskmanagement.application.task.port.in.GetTask;
import com.challenge.aitools.taskmanagement.application.task.port.in.ListTasks;
import com.challenge.aitools.taskmanagement.application.task.port.in.UpdateTask;
import com.challenge.aitools.taskmanagement.application.task.port.out.TaskRepository;
import com.challenge.aitools.taskmanagement.application.task.service.CreateTaskService;
import com.challenge.aitools.taskmanagement.application.task.service.DeleteTaskService;
import com.challenge.aitools.taskmanagement.application.task.service.GetTaskService;
import com.challenge.aitools.taskmanagement.application.task.service.ListTasksService;
import com.challenge.aitools.taskmanagement.application.task.service.UpdateTaskService;

@Configuration
public class TaskUseCaseConfig {

    @Bean
    CreateTask createTask(final TaskRepository taskRepository, final Clock clock) {
        return new CreateTaskService(taskRepository, clock);
    }

    @Bean
    ListTasks listTasks(final TaskRepository taskRepository) {
        return new ListTasksService(taskRepository);
    }

    @Bean
    GetTask getTask(final TaskRepository taskRepository) {
        return new GetTaskService(taskRepository);
    }

    @Bean
    UpdateTask updateTask(final TaskRepository taskRepository, final Clock clock) {
        return new UpdateTaskService(taskRepository, clock);
    }

    @Bean
    DeleteTask deleteTask(final TaskRepository taskRepository) {
        return new DeleteTaskService(taskRepository);
    }
}
