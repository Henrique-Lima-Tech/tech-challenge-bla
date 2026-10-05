package com.tech.challenge.domain.user.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UserTest {

    private static final String HASH = "$2a$10$abcdefghijklmnopqrstuuabcdefghijklmnopqrstuvwxyz012345";

    @Test
    void shouldCreateUserWhenDataIsValid() {
        // when
        final var user = new User(1L, "Ash", "ash@example.com", HASH);

        // then
        assertThat(user.id()).isEqualTo(1L);
        assertThat(user.name()).isEqualTo("Ash");
        assertThat(user.email()).isEqualTo("ash@example.com");
        assertThat(user.passwordHash()).isEqualTo(HASH);
    }

    @Test
    void shouldAcceptNullIdWhenUserIsNotSavedYet() {
        // when
        final var user = new User(null, "Ash", "ash@example.com", HASH);

        // then
        assertThat(user.id()).isNull();
    }

    @Test
    void shouldRejectUserWhenIdIsNotPositive() {
        // when & then
        assertThatThrownBy(() -> new User(0L, "Ash", "ash@example.com", HASH))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectUserWhenNameIsBlank() {
        // when & then
        assertThatThrownBy(() -> new User(null, " ", "ash@example.com", HASH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new User(null, null, "ash@example.com", HASH))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectUserWhenEmailIsBlank() {
        // when & then
        assertThatThrownBy(() -> new User(null, "Ash", " ", HASH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new User(null, "Ash", null, HASH))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectUserWhenPasswordHashIsBlank() {
        // when & then
        assertThatThrownBy(() -> new User(null, "Ash", "ash@example.com", " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new User(null, "Ash", "ash@example.com", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldHidePasswordHashWhenConvertedToString() {
        // given
        final var user = new User(1L, "Ash", "ash@example.com", HASH);

        // when
        final var text = user.toString();

        // then
        assertThat(text).doesNotContain(HASH).contains("id=1");
    }
}
