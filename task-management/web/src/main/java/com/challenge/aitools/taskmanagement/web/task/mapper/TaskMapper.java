package com.challenge.aitools.taskmanagement.web.task.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.challenge.aitools.taskmanagement.application.task.command.CreateTaskCommand;
import com.challenge.aitools.taskmanagement.application.task.command.UpdateTaskCommand;
import com.challenge.aitools.taskmanagement.application.task.result.TaskResult;
import com.challenge.aitools.taskmanagement.web.task.dto.request.CreateTaskRequest;
import com.challenge.aitools.taskmanagement.web.task.dto.request.UpdateTaskRequest;
import com.challenge.aitools.taskmanagement.web.task.dto.response.TaskResponse;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(target = "ownerId", source = "ownerId")
    @Mapping(target = "title", source = "request.title")
    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "status", source = "request.status")
    @Mapping(target = "dueDate", source = "request.dueDate")
    CreateTaskCommand toCommand(Long ownerId, CreateTaskRequest request);

    @Mapping(target = "ownerId", source = "ownerId")
    @Mapping(target = "taskId", source = "taskId")
    @Mapping(target = "title", source = "request.title")
    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "status", source = "request.status")
    @Mapping(target = "dueDate", source = "request.dueDate")
    UpdateTaskCommand toCommand(Long ownerId, Long taskId, UpdateTaskRequest request);

    TaskResponse toResponse(TaskResult result);
}
