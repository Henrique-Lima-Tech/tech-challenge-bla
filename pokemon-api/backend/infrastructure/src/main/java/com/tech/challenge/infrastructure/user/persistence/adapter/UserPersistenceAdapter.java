package com.tech.challenge.infrastructure.user.persistence.adapter;

import java.util.Locale;
import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.tech.challenge.application.user.port.out.UserRepositoryPort;
import com.tech.challenge.domain.user.exception.EmailAlreadyUsedException;
import com.tech.challenge.domain.user.model.User;
import com.tech.challenge.infrastructure.user.persistence.mapper.UserPersistenceMapper;
import com.tech.challenge.infrastructure.user.persistence.repository.UserJpaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepositoryPort {

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
            final var saved = userPersistenceMapper.toDomain(userJpaRepository.saveAndFlush(userPersistenceMapper.toEntity(user)));
            log.debug("Saved user {}", saved.id());
            return saved;
        } catch (final DataIntegrityViolationException e) {
            if (!isEmailConstraint(e)) {
                throw e;
            }
            // The cause is left out on purpose: the database message contains the duplicated email.
            log.warn("Saving user failed: email already used");
            throw new EmailAlreadyUsedException();
        }
    }

    private static boolean isEmailConstraint(final DataIntegrityViolationException e) {
        return e.getCause() instanceof final ConstraintViolationException violation
                && violation.getConstraintName() != null
                && violation.getConstraintName().toLowerCase(Locale.ROOT).contains(EMAIL_CONSTRAINT);
    }
}
