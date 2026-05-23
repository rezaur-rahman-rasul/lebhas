package com.lebhas.creativesaas.common.validation.timezone;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = ValidTimezoneValidator.class)
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
public @interface ValidTimezone {

    String message() default "must be a valid IANA timezone";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
