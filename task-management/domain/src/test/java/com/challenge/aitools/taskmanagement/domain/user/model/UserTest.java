package com.challenge.aitools.taskmanagement.domain.user.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.challenge.aitools.taskmanagement.domain.user.exception.InvalidUserException;

class UserTest {

    private static final String HASH = "$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123";

    @Test
    void shouldCreateUserWhenDataIsValid() {
        // when
        final var user = new User(1L, "Demo User", "demo@example.com", HASH);

        // then
        assertThat(user.id()).isEqualTo(1L);
        assertThat(user.name()).isEqualTo("Demo User");
        assertThat(user.email()).isEqualTo("demo@example.com");
        assertThat(user.passwordHash()).isEqualTo(HASH);
    }

    @Test
    void shouldAcceptUserWhenIdIsNull() {
        // when
        final var user = new User(null, "Demo User", "demo@example.com", HASH);

        // then
        assertThat(user.id()).isNull();
    }

    @Test
    void shouldNormalizeEmailWhenUserIsCreated() {
        // when
        final var user = new User(null, "Demo User", "  DEMO@Example.COM  ", HASH);

        // then
        assertThat(user.email()).isEqualTo("demo@example.com");
    }

    @Test
    void shouldTrimNameWhenUserIsCreated() {
        // when
        final var user = new User(null, "  Demo User  ", "demo@example.com", HASH);

        // then
        assertThat(user.name()).isEqualTo("Demo User");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldRejectUserWhenNameIsBlank(final String name) {
        // when & then
        assertThatThrownBy(() -> new User(null, name, "demo@example.com", HASH))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void shouldRejectUserWhenNameExceedsMaxLength() {
        // given
        final var name = "a".repeat(User.NAME_MAX_LENGTH + 1);

        // when & then
        assertThatThrownBy(() -> new User(null, name, "demo@example.com", HASH))
                .isInstanceOf(InvalidUserException.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldRejectUserWhenEmailIsBlank(final String email) {
        // when & then
        assertThatThrownBy(() -> new User(null, "Demo User", email, HASH))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void shouldRejectUserWhenEmailExceedsMaxLength() {
        // given
        final var email = "a".repeat(User.EMAIL_MAX_LENGTH) + "@example.com";

        // when & then
        assertThatThrownBy(() -> new User(null, "Demo User", email, HASH))
                .isInstanceOf(InvalidUserException.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldRejectUserWhenPasswordHashIsBlank(final String passwordHash) {
        // when & then
        assertThatThrownBy(() -> new User(null, "Demo User", "demo@example.com", passwordHash))
                .isInstanceOf(InvalidUserException.class);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void shouldRejectUserWhenIdIsNotPositive(final long id) {
        // when & then
        assertThatThrownBy(() -> new User(id, "Demo User", "demo@example.com", HASH))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void shouldHideEmailAndPasswordHashWhenConvertedToString() {
        // given
        final var user = new User(7L, "Demo User", "demo@example.com", HASH);

        // when
        final var text = user.toString();

        // then
        assertThat(text).isEqualTo("User[id=7]");
    }

    @Test
    void shouldNormalizeEmailWhenNormalizingAValue() {
        // when & then
        assertThat(User.normalizeEmail("  DEMO@Example.COM ")).isEqualTo("demo@example.com");
    }

    @Test
    void shouldReturnNullWhenNormalizingANullEmail() {
        // when & then
        assertThat(User.normalizeEmail(null)).isNull();
    }
}
