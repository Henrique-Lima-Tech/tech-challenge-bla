package com.tech.challenge.application.user.port.out;

import java.util.Optional;

import com.tech.challenge.domain.user.model.User;

public interface UserRepositoryPort {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    /**
     * @throws com.tech.challenge.domain.user.exception.EmailAlreadyUsedException if another user already has the email
     */
    User save(User user);
}
