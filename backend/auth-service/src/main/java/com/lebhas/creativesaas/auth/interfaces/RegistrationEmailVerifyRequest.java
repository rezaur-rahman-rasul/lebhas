package com.lebhas.creativesaas.auth.interfaces;

import jakarta.validation.constraints.NotBlank;

public record RegistrationEmailVerifyRequest(
        @NotBlank String registrationSessionToken,
        @NotBlank String otp
) {
}
