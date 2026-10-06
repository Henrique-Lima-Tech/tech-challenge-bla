package com.challenge.aitools.taskmanagement.application.user.port.out;

import java.util.Optional;

import com.challenge.aitools.taskmanagement.domain.user.model.User;

public interface UserRepository {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    User save(User user);
}
