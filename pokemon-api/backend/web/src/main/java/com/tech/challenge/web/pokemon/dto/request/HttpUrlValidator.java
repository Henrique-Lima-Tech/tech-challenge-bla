package com.tech.challenge.web.pokemon.dto.request;

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Null is valid: the field is optional. */
public class HttpUrlValidator implements ConstraintValidator<HttpUrl, String> {

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        try {
            final var uri = new URI(value);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (final URISyntaxException e) {
            return false;
        }
    }
}
