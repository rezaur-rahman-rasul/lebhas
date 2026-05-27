package com.lebhas.creativesaas.profile.application.dto;

import com.lebhas.creativesaas.common.validation.ValidationMessages;
import com.lebhas.creativesaas.common.validation.password.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = ValidationMessages.REQUIRED)
        String currentPassword,
        @NotBlank(message = ValidationMessages.REQUIRED)
        @StrongPassword
        String newPassword,
        @NotBlank(message = ValidationMessages.REQUIRED)
        String confirmPassword,
        boolean revokeOtherSessions
) {
}
