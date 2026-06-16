package com.lebhas.creativesaas.identity.application.dto;

public record RegistrationPasswordCommand(String registrationSessionToken, String password, String confirmPassword) {
}
