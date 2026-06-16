package com.lebhas.creativesaas.identity.application.dto;

public record RegistrationEmailStartCommand(String registrationSessionToken, String email) {
}
