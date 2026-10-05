package com.tech.challenge.application.user.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoginCommandTest {

    @Test
    void shouldHidePasswordWhenConvertedToString() {
        // given
        final var command = new LoginCommand("ash@example.com", "pikachu123");

        // when
        final var text = command.toString();

        // then
        assertThat(text).doesNotContain("pikachu123");
    }
}
