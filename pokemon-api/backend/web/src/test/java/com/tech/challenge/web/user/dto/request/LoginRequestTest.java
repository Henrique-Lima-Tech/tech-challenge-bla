package com.tech.challenge.web.user.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoginRequestTest {

    @Test
    void shouldHidePasswordWhenConvertedToString() {
        // given
        final var request = new LoginRequest("ash@example.com", "pikachu123");

        // when
        final var text = request.toString();

        // then
        assertThat(text).doesNotContain("pikachu123");
    }
}
