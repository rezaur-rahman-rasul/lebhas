package com.lebhas.creativesaas.common.validation.color;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = HexColorValidator.class)
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
public @interface HexColor {

    String message() default "must be a valid hex color";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
