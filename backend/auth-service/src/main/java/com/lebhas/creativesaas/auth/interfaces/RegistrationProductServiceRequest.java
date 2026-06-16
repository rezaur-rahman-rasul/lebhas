package com.lebhas.creativesaas.auth.interfaces;

import jakarta.validation.constraints.NotBlank;

public record RegistrationProductServiceRequest(
        @NotBlank String registrationSessionToken,
        @NotBlank String productServiceName
) {
}
