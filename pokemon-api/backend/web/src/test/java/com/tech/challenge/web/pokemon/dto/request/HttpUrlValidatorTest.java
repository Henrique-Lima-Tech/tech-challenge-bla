package com.tech.challenge.web.pokemon.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HttpUrlValidatorTest {

    private final HttpUrlValidator validator = new HttpUrlValidator();

    @ParameterizedTest
    @ValueSource(strings = { "https://img/25.png", "http://raw.githubusercontent.com/PokeAPI/sprites/25.png",
            "HTTPS://IMG/25.PNG" })
    void shouldAcceptUrlWhenSchemeIsHttpOrHttpsAndHostIsPresent(final String url) {
        // when
        final var valid = validator.isValid(url, null);

        // then
        assertThat(valid).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "ftp://img/25.png", "javascript:alert(1)", "https://", "/img/25.png", "https://img/a b.png",
            "" })
    void shouldRejectUrlWhenSchemeHostOrSyntaxIsInvalid(final String url) {
        // when
        final var valid = validator.isValid(url, null);

        // then
        assertThat(valid).isFalse();
    }

    @Test
    void shouldAcceptNullWhenFieldIsOptional() {
        // when
        final var valid = validator.isValid(null, null);

        // then
        assertThat(valid).isTrue();
    }
}
