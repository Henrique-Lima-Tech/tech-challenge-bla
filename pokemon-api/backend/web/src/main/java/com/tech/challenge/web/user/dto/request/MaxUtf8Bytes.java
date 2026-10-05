package com.tech.challenge.web.user.dto.request;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Limits the UTF-8 size of a text. BCrypt refuses to hash more than 72 bytes, which {@code @Size} (characters)
 * cannot express.
 */
@Documented
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxUtf8Bytes {

    int value();

    String message();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
