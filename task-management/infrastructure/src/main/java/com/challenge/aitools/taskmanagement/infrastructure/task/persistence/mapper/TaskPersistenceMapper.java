package com.challenge.aitools.taskmanagement.infrastructure.task.persistence.mapper;

import org.mapstruct.Mapper;

import com.challenge.aitools.taskmanagement.domain.task.model.Task;
import com.challenge.aitools.taskmanagement.infrastructure.task.persistence.entity.TaskEntity;

@Mapper(componentModel = "spring")
public interface TaskPersistenceMapper {

    Task toDomain(TaskEntity entity);

    TaskEntity toEntity(Task task);
}
