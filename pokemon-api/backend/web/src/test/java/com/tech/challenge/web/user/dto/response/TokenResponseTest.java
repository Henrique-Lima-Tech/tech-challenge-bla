package com.tech.challenge.web.user.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenResponseTest {

    @Test
    void shouldHideTokenWhenConvertedToString() {
        // given
        final var response = new TokenResponse("signed.jwt.token", "Bearer", 3600, "Ash");

        // when
        final var text = response.toString();

        // then
        assertThat(text).doesNotContain("signed.jwt.token");
    }
}
