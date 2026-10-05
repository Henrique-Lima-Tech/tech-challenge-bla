package com.challenge.aitools.taskmanagement.infrastructure.user.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.challenge.aitools.taskmanagement.domain.user.exception.EmailAlreadyRegisteredException;
import com.challenge.aitools.taskmanagement.domain.user.model.User;
import com.challenge.aitools.taskmanagement.infrastructure.user.persistence.mapper.UserPersistenceMapperImpl;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Import({ UserRepositoryAdapter.class, UserPersistenceMapperImpl.class })
class UserRepositoryAdapterTest {

    private static final String HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5BWX4Z3Pc5lYxOZpSmVnZf2C1Q/4K";

    private final UserRepositoryAdapter adapter;

    @Autowired
    UserRepositoryAdapterTest(final UserRepositoryAdapter adapter) {
        this.adapter = adapter;
    }

    @Test
    void shouldAssignIdWhenUserIsSaved() {
        // when
        final var saved = adapter.save(new User(null, "Demo User", "demo@example.com", HASH));

        // then
        assertThat(saved.id()).isPositive();
        assertThat(saved.name()).isEqualTo("Demo User");
        assertThat(saved.email()).isEqualTo("demo@example.com");
        assertThat(saved.passwordHash()).isEqualTo(HASH);
    }

    @Test
    void shouldFindUserWhenEmailIsKnown() {
        // given
        adapter.save(new User(null, "Demo User", "demo@example.com", HASH));

        // when
        final var found = adapter.findByEmail("demo@example.com");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("Demo User");
    }

    @Test
    void shouldFindNothingWhenEmailIsUnknown() {
        // when & then
        assertThat(adapter.findByEmail("missing@example.com")).isEmpty();
    }

    @Test
    void shouldReportEmailAsTakenWhenUserExists() {
        // given
        adapter.save(new User(null, "Demo User", "demo@example.com", HASH));

        // when & then
        assertThat(adapter.existsByEmail("demo@example.com")).isTrue();
        assertThat(adapter.existsByEmail("other@example.com")).isFalse();
    }

    @Test
    void shouldRejectSaveWhenEmailIsAlreadyRegistered() {
        // given
        adapter.save(new User(null, "Demo User", "demo@example.com", HASH));

        // when & then
        assertThatThrownBy(() -> adapter.save(new User(null, "Other User", "demo@example.com", HASH)))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("Email already registered");
    }
}
