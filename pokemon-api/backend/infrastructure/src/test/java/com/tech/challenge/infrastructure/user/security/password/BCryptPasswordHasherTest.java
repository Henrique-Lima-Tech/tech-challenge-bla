package com.tech.challenge.infrastructure.user.security.password;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void shouldProduceBcryptHashWhenPasswordIsHashed() {
        // when
        final var hash = hasher.hash("pikachu123");

        // then
        assertThat(hash).startsWith("$2a$10$").hasSize(60).doesNotContain("pikachu123");
    }

    @Test
    void shouldProduceDifferentHashesWhenSamePasswordIsHashedTwice() {
        // when
        final var first = hasher.hash("pikachu123");
        final var second = hasher.hash("pikachu123");

        // then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void shouldMatchWhenPasswordIsTheHashedOne() {
        // given
        final var hash = hasher.hash("pikachu123");

        // when & then
        assertThat(hasher.matches("pikachu123", hash)).isTrue();
    }

    @Test
    void shouldNotMatchWhenPasswordIsDifferent() {
        // given
        final var hash = hasher.hash("pikachu123");

        // when & then
        assertThat(hasher.matches("pikachu124", hash)).isFalse();
        assertThat(hasher.matches(" pikachu123", hash)).isFalse();
    }

    @Test
    void shouldNotMatchWithoutFailingWhenPasswordIsLongerThan72Bytes() {
        // given
        final var hash = hasher.hash("pikachu123");

        // when & then
        assertThat(hasher.matches("x".repeat(100), hash)).isFalse();
    }
}
