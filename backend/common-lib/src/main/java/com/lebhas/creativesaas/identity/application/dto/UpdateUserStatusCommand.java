package com.lebhas.creativesaas.identity.application.dto;

import com.lebhas.creativesaas.identity.domain.UserStatus;

import java.util.UUID;

public record UpdateUserStatusCommand(
        UUID userId,
        UserStatus status
) {
}
