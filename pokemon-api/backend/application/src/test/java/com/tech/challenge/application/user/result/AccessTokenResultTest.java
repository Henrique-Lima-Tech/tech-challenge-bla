package com.tech.challenge.application.user.result;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AccessTokenResultTest {

    @Test
    void shouldHideTokenWhenConvertedToString() {
        // given
        final var result = new AccessTokenResult("signed.jwt.token", 3600);

        // when
        final var text = result.toString();

        // then
        assertThat(text).doesNotContain("signed.jwt.token");
    }
}
