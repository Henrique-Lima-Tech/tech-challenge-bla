package com.tech.challenge.web.pokemon.dto.request;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * No two texts of a list are equal ignoring case (D-27, {@code internalTags}). Hibernate Validator's
 * {@code @UniqueElements} compares with {@code equals}.
 */
@Documented
@Constraint(validatedBy = UniqueIgnoringCaseValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueIgnoringCase {

    String message();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
