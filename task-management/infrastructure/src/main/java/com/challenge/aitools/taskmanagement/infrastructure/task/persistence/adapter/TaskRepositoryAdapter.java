package com.challenge.aitools.taskmanagement.infrastructure.task.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.challenge.aitools.taskmanagement.application.shared.pagination.Page;
import com.challenge.aitools.taskmanagement.application.task.port.out.TaskRepository;
import com.challenge.aitools.taskmanagement.domain.task.model.Task;
import com.challenge.aitools.taskmanagement.domain.task.model.TaskStatus;
import com.challenge.aitools.taskmanagement.infrastructure.task.persistence.entity.TaskEntity;
import com.challenge.aitools.taskmanagement.infrastructure.task.persistence.mapper.TaskPersistenceMapper;
import com.challenge.aitools.taskmanagement.infrastructure.task.persistence.repository.TaskJpaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskRepositoryAdapter implements TaskRepository {

    private static final Sort SORT = Sort.by(Sort.Order.asc("dueDate"), Sort.Order.asc("id"));

    private final TaskJpaRepository taskJpaRepository;
    private final TaskPersistenceMapper taskPersistenceMapper;

    @Override
    public Task save(final Task task) {
        final var entity = taskJpaRepository.saveAndFlush(taskPersistenceMapper.toEntity(task));
        log.debug("Saved task {} of owner {}", entity.getId(), entity.getOwnerId());
        return taskPersistenceMapper.toDomain(entity);
    }

    @Override
    public Optional<Task> findByIdAndOwnerId(final Long id, final Long ownerId) {
        return taskJpaRepository.findByIdAndOwnerId(id, ownerId).map(taskPersistenceMapper::toDomain);
    }

    @Override
    public Page<Task> findByOwnerId(final Long ownerId, final TaskStatus status, final int page, final int size) {
        final var pageable = PageRequest.of(page, size, SORT);
        final var found = status == null
                ? taskJpaRepository.findByOwnerId(ownerId, pageable)
                : taskJpaRepository.findByOwnerIdAndStatus(ownerId, status, pageable);
        log.debug("Found {} of {} tasks of owner {}", found.getNumberOfElements(), found.getTotalElements(), ownerId);
        return new Page<>(found.getContent().stream().map(taskPersistenceMapper::toDomain).toList(),
                found.getNumber(), found.getSize(), found.getTotalElements(), found.getTotalPages());
    }

    @Override
    public void delete(final Task task) {
        taskJpaRepository.deleteById(task.id());
        log.debug("Deleted task {} of owner {}", task.id(), task.ownerId());
    }
}
