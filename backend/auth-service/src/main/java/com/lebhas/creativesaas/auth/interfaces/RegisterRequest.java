package com.lebhas.creativesaas.auth.interfaces;

import com.lebhas.creativesaas.common.validation.ValidationMessages;
import com.lebhas.creativesaas.common.validation.password.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RegisterRequest(
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Size(max = 80)
        String firstName,
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Size(max = 80)
        String lastName,
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Email
        @Size(max = 160)
        String email,
        @Size(max = 30)
        String phone,
        @NotBlank(message = ValidationMessages.REQUIRED)
        @StrongPassword
        String password,
        UUID workspaceId,
        String invitationToken
) {
}
