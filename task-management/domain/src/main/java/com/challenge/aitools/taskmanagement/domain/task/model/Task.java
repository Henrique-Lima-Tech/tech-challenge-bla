package com.challenge.aitools.taskmanagement.domain.task.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import com.challenge.aitools.taskmanagement.domain.task.exception.InvalidTaskException;

public record Task(Long id, String title, String description, TaskStatus status, LocalDate dueDate, Long ownerId,
        Instant createdAt, Instant updatedAt) {

    public static final int TITLE_MAX_LENGTH = 120;
    public static final int DESCRIPTION_MAX_LENGTH = 2000;

    private static final ZoneOffset ZONE = ZoneOffset.UTC;

    public Task {
        if (id != null && id <= 0) {
            throw new InvalidTaskException("id", "must be positive");
        }
        if (title == null || title.isBlank()) {
            throw new InvalidTaskException("title", "must not be blank");
        }
        if (status == null) {
            throw new InvalidTaskException("status", "must not be null");
        }
        if (dueDate == null) {
            throw new InvalidTaskException("dueDate", "must not be null");
        }
        if (ownerId == null || ownerId <= 0) {
            throw new InvalidTaskException("ownerId", "must be a positive id");
        }
        if (createdAt == null) {
            throw new InvalidTaskException("createdAt", "must not be null");
        }
        if (updatedAt == null) {
            throw new InvalidTaskException("updatedAt", "must not be null");
        }
        title = title.trim();
        description = normalizeDescription(description);
        if (title.length() > TITLE_MAX_LENGTH) {
            throw new InvalidTaskException("title", "must be at most " + TITLE_MAX_LENGTH + " characters");
        }
        if (description != null && description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new InvalidTaskException("description", "must be at most " + DESCRIPTION_MAX_LENGTH + " characters");
        }
    }

    public static Task create(final String title, final String description, final TaskStatus status,
            final LocalDate dueDate, final Long ownerId, final Instant now) {
        requireInstant(now);
        requireNotInThePast(dueDate, now);
        return new Task(null, title, description, statusOrDefault(status), dueDate, ownerId, now, now);
    }

    public Task replace(final String title, final String description, final TaskStatus status,
            final LocalDate dueDate, final Instant now) {
        requireInstant(now);
        if (!this.dueDate.equals(dueDate)) {
            requireNotInThePast(dueDate, now);
        }
        return new Task(id, title, description, statusOrDefault(status), dueDate, ownerId, createdAt, now);
    }

    private static TaskStatus statusOrDefault(final TaskStatus status) {
        return status == null ? TaskStatus.TODO : status;
    }

    private static String normalizeDescription(final String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }

    private static void requireInstant(final Instant now) {
        if (now == null) {
            throw new InvalidTaskException("updatedAt", "must not be null");
        }
    }

    private static void requireNotInThePast(final LocalDate dueDate, final Instant now) {
        if (dueDate == null) {
            throw new InvalidTaskException("dueDate", "must not be null");
        }
        if (dueDate.isBefore(LocalDate.ofInstant(now, ZONE))) {
            throw new InvalidTaskException("dueDate", "must not be in the past");
        }
    }
}
