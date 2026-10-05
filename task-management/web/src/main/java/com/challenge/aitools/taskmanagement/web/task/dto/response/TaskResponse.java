package com.challenge.aitools.taskmanagement.web.task.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(Long id, String title, String description, String status, LocalDate dueDate,
        Instant createdAt, Instant updatedAt) {
}
