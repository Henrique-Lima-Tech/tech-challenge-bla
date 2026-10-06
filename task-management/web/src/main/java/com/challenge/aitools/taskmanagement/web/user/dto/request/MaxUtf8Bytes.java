package com.challenge.aitools.taskmanagement.web.user.dto.request;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
@Target({ java.lang.annotation.ElementType.FIELD, java.lang.annotation.ElementType.PARAMETER,
        java.lang.annotation.ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxUtf8Bytes {

    int value();

    String message();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
