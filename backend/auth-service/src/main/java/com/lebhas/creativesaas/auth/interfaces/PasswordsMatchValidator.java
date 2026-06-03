package com.lebhas.creativesaas.auth.interfaces;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, RegisterRequest> {

    @Override
    public boolean isValid(RegisterRequest request, ConstraintValidatorContext context) {
        if (request == null || request.password() == null || request.confirmPassword() == null) {
            return true;
        }

        if (request.password().equals(request.confirmPassword())) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("Password and confirm password do not match.")
                .addPropertyNode("confirmPassword")
                .addConstraintViolation();
        return false;
    }
}
