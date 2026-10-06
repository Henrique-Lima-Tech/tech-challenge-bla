package com.tech.challenge.web.pokemon.dto.request;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UniqueIgnoringCaseValidator implements ConstraintValidator<UniqueIgnoringCase, List<String>> {

    @Override
    public boolean isValid(final List<String> values, final ConstraintValidatorContext context) {
        if (values == null) {
            return true;
        }
        final var texts = values.stream().filter(Objects::nonNull).toList();
        return texts.stream().map(text -> text.toLowerCase(Locale.ROOT)).distinct().count() == texts.size();
    }
}
