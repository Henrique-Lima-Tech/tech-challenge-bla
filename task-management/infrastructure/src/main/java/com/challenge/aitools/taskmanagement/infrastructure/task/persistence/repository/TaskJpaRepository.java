package com.challenge.aitools.taskmanagement.infrastructure.task.persistence.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.challenge.aitools.taskmanagement.domain.task.model.TaskStatus;
import com.challenge.aitools.taskmanagement.infrastructure.task.persistence.entity.TaskEntity;

public interface TaskJpaRepository extends JpaRepository<TaskEntity, Long> {

    Optional<TaskEntity> findByIdAndOwnerId(Long id, Long ownerId);

    Page<TaskEntity> findByOwnerId(Long ownerId, Pageable pageable);

    Page<TaskEntity> findByOwnerIdAndStatus(Long ownerId, TaskStatus status, Pageable pageable);
}
