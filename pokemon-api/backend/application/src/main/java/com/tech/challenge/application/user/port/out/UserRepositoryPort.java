package com.tech.challenge.application.user.port.out;

import java.util.Optional;

import com.tech.challenge.domain.user.model.User;

public interface UserRepositoryPort {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    User save(User user);
}
