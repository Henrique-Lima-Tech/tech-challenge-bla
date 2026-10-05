package com.challenge.aitools.taskmanagement.web.user.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MaxUtf8BytesValidatorTest {

    private final MaxUtf8BytesValidator validator = validatorWithMax(72);

    @Test
    void shouldAcceptWhenValueIsNull() {
        // when & then
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void shouldAcceptWhenValueIsAtTheByteLimit() {
        // given
        final var value = "a".repeat(72);

        // when & then
        assertThat(validator.isValid(value, null)).isTrue();
    }

    @Test
    void shouldRejectWhenValueIsOverTheByteLimit() {
        // given
        final var value = "a".repeat(73);

        // when & then
        assertThat(validator.isValid(value, null)).isFalse();
    }

    @Test
    void shouldCountBytesNotCharactersWhenValueIsMultiByte() {
        // given
        final var value = "é".repeat(40);

        // when & then
        assertThat(value).hasSize(40);
        assertThat(validator.isValid(value, null)).isFalse();
    }

    private static MaxUtf8BytesValidator validatorWithMax(final int max) {
        final var validator = new MaxUtf8BytesValidator();
        validator.initialize(new MaxUtf8Bytes() {

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return MaxUtf8Bytes.class;
            }

            @Override
            public int value() {
                return max;
            }

            @Override
            public String message() {
                return "must be at most " + max + " bytes";
            }

            @Override
            public Class<?>[] groups() {
                return new Class<?>[0];
            }

            @Override
            public Class<? extends jakarta.validation.Payload>[] payload() {
                return newPayloadArray();
            }

            @SuppressWarnings("unchecked")
            private Class<? extends jakarta.validation.Payload>[] newPayloadArray() {
                return new Class[0];
            }
        });
        return validator;
    }
}
