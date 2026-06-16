package com.lebhas.creativesaas.auth.interfaces;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegistrationEmailStartRequest(
        @NotBlank String registrationSessionToken,
        @Email @NotBlank String email
) {
}
