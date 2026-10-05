package com.tech.challenge.infrastructure.user.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.tech.challenge.domain.user.exception.EmailAlreadyUsedException;
import com.tech.challenge.domain.user.model.User;
import com.tech.challenge.infrastructure.user.persistence.mapper.UserPersistenceMapperImpl;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Import({ UserPersistenceAdapter.class, UserPersistenceMapperImpl.class })
class UserPersistenceAdapterTest {

    private static final String HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5BWX4Z3Pc5lYxOZpSmVnZf2C1Q/4K";

    private final UserPersistenceAdapter adapter;

    @Autowired
    UserPersistenceAdapterTest(final UserPersistenceAdapter adapter) {
        this.adapter = adapter;
    }

    @Test
    void shouldAssignIdWhenUserIsSaved() {
        // when
        final var saved = adapter.save(new User(null, "Ash", "ash@example.com", HASH));

        // then
        assertThat(saved.id()).isPositive();
        assertThat(saved.name()).isEqualTo("Ash");
        assertThat(saved.email()).isEqualTo("ash@example.com");
        assertThat(saved.passwordHash()).isEqualTo(HASH);
    }

    @Test
    void shouldFindUserWhenEmailIsStored() {
        // given
        final var saved = adapter.save(new User(null, "Ash", "ash@example.com", HASH));

        // when
        final var found = adapter.findByEmail("ash@example.com");

        // then
        assertThat(found).contains(saved);
    }

    @Test
    void shouldReturnEmptyWhenEmailIsNotStored() {
        // when
        final var found = adapter.findByEmail("misty@example.com");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldReportExistenceWhenEmailIsStored() {
        // given
        adapter.save(new User(null, "Ash", "ash@example.com", HASH));

        // when & then
        assertThat(adapter.existsByEmail("ash@example.com")).isTrue();
        assertThat(adapter.existsByEmail("misty@example.com")).isFalse();
    }

    @Test
    void shouldThrowEmailAlreadyUsedWhenEmailIsTaken() {
        // given
        adapter.save(new User(null, "Ash", "ash@example.com", HASH));

        // when & then
        assertThatThrownBy(() -> adapter.save(new User(null, "Ash Ketchum", "ash@example.com", HASH)))
                .isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    void shouldRethrowWhenIntegrityViolationIsNotTheEmailConstraint() {
        // given
        final var tooLongName = "a".repeat(101);

        // when & then
        assertThatThrownBy(() -> adapter.save(new User(null, tooLongName, "ash@example.com", HASH)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
