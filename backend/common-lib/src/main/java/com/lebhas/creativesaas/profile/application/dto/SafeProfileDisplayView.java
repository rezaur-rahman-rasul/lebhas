package com.lebhas.creativesaas.profile.application.dto;

import com.lebhas.creativesaas.common.security.Role;

import java.util.UUID;

public record SafeProfileDisplayView(
        UUID userId,
        String displayName,
        String profileImageUrl,
        Role role
) {
}
