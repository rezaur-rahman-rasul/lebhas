package com.lebhas.creativesaas.identity.application.dto;

public record RegistrationProductServiceCommand(
        String registrationSessionToken,
        String productServiceName
) {
}
