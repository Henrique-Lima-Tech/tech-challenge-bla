package com.challenge.aitools.taskmanagement.infrastructure.user.persistence.mapper;

import org.mapstruct.Mapper;

import com.challenge.aitools.taskmanagement.domain.user.model.User;
import com.challenge.aitools.taskmanagement.infrastructure.user.persistence.entity.UserEntity;

@Mapper(componentModel = "spring")
public interface UserPersistenceMapper {

    User toDomain(UserEntity entity);

    UserEntity toEntity(User user);
}
