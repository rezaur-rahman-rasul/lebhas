package com.lebhas.creativesaas.auth.interfaces;

import jakarta.validation.constraints.NotBlank;

public record RegistrationBrandRequest(
        @NotBlank String registrationSessionToken,
        @NotBlank String brandName,
        String deviceId
) {
}
