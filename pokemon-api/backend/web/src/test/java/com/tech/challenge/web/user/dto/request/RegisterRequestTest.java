package com.tech.challenge.web.user.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RegisterRequestTest {

    @Test
    void shouldHidePasswordWhenConvertedToString() {
        // given
        final var request = new RegisterRequest("Ash", "ash@example.com", "pikachu123");

        // when
        final var text = request.toString();

        // then
        assertThat(text).doesNotContain("pikachu123");
    }
}
