package com.tech.challenge.web.pokemon.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class UniqueIgnoringCaseValidatorTest {

    private final UniqueIgnoringCaseValidator validator = new UniqueIgnoringCaseValidator();

    @Test
    void shouldAcceptListWhenEveryTextIsDifferent() {
        // when
        final var valid = validator.isValid(List.of("starter", "electric", "kanto"), null);

        // then
        assertThat(valid).isTrue();
    }

    @Test
    void shouldRejectListWhenTwoTextsDifferOnlyInCase() {
        // when
        final var valid = validator.isValid(List.of("Starter", "electric", "STARTER"), null);

        // then
        assertThat(valid).isFalse();
    }

    @Test
    void shouldAcceptNullAndEmptyListsWhenFieldIsOptional() {
        // when & then
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid(List.of(), null)).isTrue();
    }

    @Test
    void shouldSkipNullElementsWhenCheckingDuplicates() {
        // given
        final var tags = Arrays.asList("starter", null, null, "electric");

        // when
        final var valid = validator.isValid(tags, null);

        // then
        assertThat(valid).isTrue();
    }
}
