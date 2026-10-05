package com.challenge.aitools.taskmanagement.web.task.dto.request;

import java.time.LocalDate;

import com.challenge.aitools.taskmanagement.domain.task.model.Task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(

        @NotBlank(message = "must not be blank")
        @Size(max = Task.TITLE_MAX_LENGTH, message = "must be at most " + Task.TITLE_MAX_LENGTH + " characters")
        String title,

        @Size(max = Task.DESCRIPTION_MAX_LENGTH,
                message = "must be at most " + Task.DESCRIPTION_MAX_LENGTH + " characters")
        String description,

        @Pattern(regexp = TaskStatusPattern.REGEXP, message = TaskStatusPattern.MESSAGE)
        String status,

        @NotNull(message = "must not be null")
        LocalDate dueDate) {
}
