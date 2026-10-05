package com.challenge.aitools.taskmanagement.web.task.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.challenge.aitools.taskmanagement.application.task.command.DeleteTaskCommand;
import com.challenge.aitools.taskmanagement.application.task.command.GetTaskCommand;
import com.challenge.aitools.taskmanagement.application.task.command.ListTasksCommand;
import com.challenge.aitools.taskmanagement.application.task.port.in.CreateTask;
import com.challenge.aitools.taskmanagement.application.task.port.in.DeleteTask;
import com.challenge.aitools.taskmanagement.application.task.port.in.GetTask;
import com.challenge.aitools.taskmanagement.application.task.port.in.ListTasks;
import com.challenge.aitools.taskmanagement.application.task.port.in.UpdateTask;
import com.challenge.aitools.taskmanagement.domain.task.model.TaskStatus;
import com.challenge.aitools.taskmanagement.web.shared.pagination.PageResponse;
import com.challenge.aitools.taskmanagement.web.shared.security.AuthenticatedUser;
import com.challenge.aitools.taskmanagement.web.task.dto.request.CreateTaskRequest;
import com.challenge.aitools.taskmanagement.web.task.dto.request.TaskStatusPattern;
import com.challenge.aitools.taskmanagement.web.task.dto.request.UpdateTaskRequest;
import com.challenge.aitools.taskmanagement.web.task.dto.response.TaskResponse;
import com.challenge.aitools.taskmanagement.web.task.mapper.TaskMapper;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private static final String PATH = "/api/v1/tasks/";

    private final CreateTask createTask;
    private final ListTasks listTasks;
    private final GetTask getTask;
    private final UpdateTask updateTask;
    private final DeleteTask deleteTask;
    private final TaskMapper taskMapper;

    @PostMapping
    ResponseEntity<TaskResponse> create(@AuthenticationPrincipal final Jwt jwt,
            @Valid @RequestBody final CreateTaskRequest request) {
        final var ownerId = AuthenticatedUser.idOf(jwt);
        log.debug("Creating a task for owner {}", ownerId);
        final var response = taskMapper.toResponse(createTask.handle(taskMapper.toCommand(ownerId, request)));
        return ResponseEntity.created(URI.create(PATH + response.id())).body(response);
    }

    @GetMapping
    PageResponse<TaskResponse> list(@AuthenticationPrincipal final Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "must be at least 0") final int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "must be at least 1")
            @Max(value = 100, message = "must be at most 100") final int size,
            @RequestParam(required = false)
            @Pattern(regexp = TaskStatusPattern.REGEXP, message = TaskStatusPattern.MESSAGE) final String status) {
        final var ownerId = AuthenticatedUser.idOf(jwt);
        log.debug("Listing tasks of owner {}: page {}, size {}, status {}", ownerId, page, size, status);
        final var command = new ListTasksCommand(ownerId, status == null ? null : TaskStatus.valueOf(status), page, size);
        return PageResponse.from(listTasks.handle(command).map(taskMapper::toResponse));
    }

    @GetMapping("/{id}")
    TaskResponse get(@AuthenticationPrincipal final Jwt jwt,
            @PathVariable @Min(value = 1, message = "must be at least 1") final Long id) {
        final var ownerId = AuthenticatedUser.idOf(jwt);
        log.debug("Reading task {} of owner {}", id, ownerId);
        return taskMapper.toResponse(getTask.handle(new GetTaskCommand(ownerId, id)));
    }

    @PutMapping("/{id}")
    TaskResponse update(@AuthenticationPrincipal final Jwt jwt,
            @PathVariable @Min(value = 1, message = "must be at least 1") final Long id,
            @Valid @RequestBody final UpdateTaskRequest request) {
        final var ownerId = AuthenticatedUser.idOf(jwt);
        log.debug("Replacing task {} of owner {}", id, ownerId);
        return taskMapper.toResponse(updateTask.handle(taskMapper.toCommand(ownerId, id, request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal final Jwt jwt,
            @PathVariable @Min(value = 1, message = "must be at least 1") final Long id) {
        final var ownerId = AuthenticatedUser.idOf(jwt);
        log.debug("Deleting task {} of owner {}", id, ownerId);
        deleteTask.handle(new DeleteTaskCommand(ownerId, id));
    }
}
