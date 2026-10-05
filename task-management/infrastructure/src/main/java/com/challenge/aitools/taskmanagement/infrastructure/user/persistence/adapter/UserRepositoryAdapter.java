package com.challenge.aitools.taskmanagement.infrastructure.user.persistence.adapter;

import java.util.Locale;
import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.challenge.aitools.taskmanagement.application.user.port.out.UserRepository;
import com.challenge.aitools.taskmanagement.domain.user.exception.EmailAlreadyRegisteredException;
import com.challenge.aitools.taskmanagement.domain.user.model.User;
import com.challenge.aitools.taskmanagement.infrastructure.user.persistence.mapper.UserPersistenceMapper;
import com.challenge.aitools.taskmanagement.infrastructure.user.persistence.repository.UserJpaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    /** Name given in {@code V1__create_users_table.sql}. */
    private static final String EMAIL_CONSTRAINT = "uk_users_email";

    private final UserJpaRepository userJpaRepository;
    private final UserPersistenceMapper userPersistenceMapper;

    @Override
    public boolean existsByEmail(final String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public Optional<User> findByEmail(final String email) {
        return userJpaRepository.findByEmail(email).map(userPersistenceMapper::toDomain);
    }

    @Override
    public User save(final User user) {
        try {
            final var entity = userJpaRepository.saveAndFlush(userPersistenceMapper.toEntity(user));
            log.debug("Saved user {}", entity.getId());
            return userPersistenceMapper.toDomain(entity);
        } catch (final DataIntegrityViolationException e) {
            if (!isEmailConstraint(e)) {
                throw e;
            }
            // The cause is left out on purpose: the database message carries the duplicated email.
            log.warn("Saving user failed: email already registered");
            throw new EmailAlreadyRegisteredException();
        }
    }

    private static boolean isEmailConstraint(final DataIntegrityViolationException e) {
        return e.getCause() instanceof final ConstraintViolationException violation
                && violation.getConstraintName() != null
                && violation.getConstraintName().toLowerCase(Locale.ROOT).contains(EMAIL_CONSTRAINT);
    }
}
