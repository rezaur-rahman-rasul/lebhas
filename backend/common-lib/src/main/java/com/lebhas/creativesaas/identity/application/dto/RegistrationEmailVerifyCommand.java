package com.lebhas.creativesaas.identity.application.dto;

public record RegistrationEmailVerifyCommand(String registrationSessionToken, String otp) {
}
