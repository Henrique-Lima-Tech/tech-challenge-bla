package com.tech.challenge.web.pokemon.dto.request;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * An absolute {@code http} or {@code https} URL with a host (D-27, {@code spriteUrl}). Hibernate Validator's
 * {@code @URL} can pin only one protocol at a time.
 */
@Documented
@Constraint(validatedBy = HttpUrlValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpUrl {

    String message();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
