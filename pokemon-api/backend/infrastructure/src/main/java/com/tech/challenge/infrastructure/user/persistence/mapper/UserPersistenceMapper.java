package com.tech.challenge.infrastructure.user.persistence.mapper;

import org.mapstruct.Mapper;

import com.tech.challenge.domain.user.model.User;
import com.tech.challenge.infrastructure.user.persistence.entity.UserEntity;

@Mapper(componentModel = "spring")
public interface UserPersistenceMapper {

    User toDomain(UserEntity entity);

    UserEntity toEntity(User user);
}
