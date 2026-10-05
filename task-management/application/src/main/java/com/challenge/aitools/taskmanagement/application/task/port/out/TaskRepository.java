package com.challenge.aitools.taskmanagement.application.task.port.out;

import java.util.Optional;

import com.challenge.aitools.taskmanagement.application.shared.pagination.Page;
import com.challenge.aitools.taskmanagement.domain.task.model.Task;
import com.challenge.aitools.taskmanagement.domain.task.model.TaskStatus;

public interface TaskRepository {

    Task save(Task task);

    Optional<Task> findByIdAndOwnerId(Long id, Long ownerId);

    /**
     * @param status when null, every status is returned
     */
    Page<Task> findByOwnerId(Long ownerId, TaskStatus status, int page, int size);

    void delete(Task task);
}
