package com.challenge.aitools.taskmanagement.infrastructure.user.security.password;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void shouldProduceABCryptHashWhenHashing() {
        // when
        final var hash = hasher.hash("password123");

        // then
        assertThat(hash).startsWith("$2a$").hasSize(60);
    }

    @Test
    void shouldNeverReturnTheRawPasswordWhenHashing() {
        // when
        final var hash = hasher.hash("password123");

        // then
        assertThat(hash).doesNotContain("password123");
    }

    @Test
    void shouldUseADifferentSaltWhenHashingTheSamePasswordTwice() {
        // when
        final var first = hasher.hash("password123");
        final var second = hasher.hash("password123");

        // then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void shouldMatchWhenPasswordIsCorrect() {
        // given
        final var hash = hasher.hash("password123");

        // when & then
        assertThat(hasher.matches("password123", hash)).isTrue();
    }

    @Test
    void shouldNotMatchWhenPasswordIsWrong() {
        // given
        final var hash = hasher.hash("password123");

        // when & then
        assertThat(hasher.matches("wrong-password", hash)).isFalse();
    }
}
