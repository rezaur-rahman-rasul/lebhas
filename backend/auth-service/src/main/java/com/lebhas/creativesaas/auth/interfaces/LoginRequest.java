package com.lebhas.creativesaas.auth.interfaces;

import com.lebhas.creativesaas.common.validation.ValidationMessages;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record LoginRequest(
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Email
        @Size(max = 160)
        String email,
        @NotBlank(message = ValidationMessages.REQUIRED)
        String password,
        @Size(max = 120)
        String deviceId,
        UUID workspaceId
) {
}
