package com.lebhas.creativesaas.auth.interfaces;

import jakarta.validation.constraints.NotBlank;

public record RegistrationSessionRequest(@NotBlank String registrationSessionToken) {
}
