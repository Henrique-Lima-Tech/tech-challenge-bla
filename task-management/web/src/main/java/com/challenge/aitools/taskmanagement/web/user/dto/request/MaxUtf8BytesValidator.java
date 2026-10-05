package com.challenge.aitools.taskmanagement.web.user.dto.request;

import java.nio.charset.StandardCharsets;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MaxUtf8BytesValidator implements ConstraintValidator<MaxUtf8Bytes, String> {

    private int maxBytes;

    @Override
    public void initialize(final MaxUtf8Bytes constraint) {
        this.maxBytes = constraint.value();
    }

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        return value == null || value.getBytes(StandardCharsets.UTF_8).length <= maxBytes;
    }
}
