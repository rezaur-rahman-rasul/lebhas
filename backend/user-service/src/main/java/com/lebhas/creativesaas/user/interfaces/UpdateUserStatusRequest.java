package com.lebhas.creativesaas.user.interfaces;

import com.lebhas.creativesaas.common.validation.ValidationMessages;
import com.lebhas.creativesaas.identity.domain.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull(message = ValidationMessages.REQUIRED)
        UserStatus status
) {
}
