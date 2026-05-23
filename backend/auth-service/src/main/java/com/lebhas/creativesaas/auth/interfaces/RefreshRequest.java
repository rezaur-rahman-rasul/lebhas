package com.lebhas.creativesaas.auth.interfaces;

import com.lebhas.creativesaas.common.validation.ValidationMessages;
import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = ValidationMessages.REQUIRED)
        String refreshToken,
        String deviceId
) {
}
