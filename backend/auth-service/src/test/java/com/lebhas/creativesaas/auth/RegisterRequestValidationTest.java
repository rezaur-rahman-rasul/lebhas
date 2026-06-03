package com.lebhas.creativesaas.auth;

import com.lebhas.creativesaas.auth.interfaces.RegisterRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldReturnConfirmPasswordFieldErrorWhenMissing() {
        RegisterRequest request = validRequest("StrongP@ssw0rd!", null);

        Set<String> fields = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(fields).contains("confirmPassword");
    }

    @Test
    void shouldReturnConfirmPasswordFieldErrorWhenPasswordDoesNotMatch() {
        RegisterRequest request = validRequest("StrongP@ssw0rd!", "DifferentP@ssw0rd!");

        var violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath().toString()).isEqualTo("confirmPassword");
                    assertThat(violation.getMessage()).isEqualTo("Password and confirm password do not match.");
                });
    }

    @Test
    void shouldAcceptSuccessfulRegisterPayloadWithConfirmPassword() {
        RegisterRequest request = validRequest("StrongP@ssw0rd!", "StrongP@ssw0rd!");

        assertThat(validator.validate(request)).isEmpty();
    }

    private RegisterRequest validRequest(String password, String confirmPassword) {
        return new RegisterRequest(
                "Hridoy",
                "Bhuiyan",
                "hridoy.bhuiyan12@example.com",
                "+1234567890",
                null,
                password,
                confirmPassword,
                null,
                null,
                null);
    }
}
