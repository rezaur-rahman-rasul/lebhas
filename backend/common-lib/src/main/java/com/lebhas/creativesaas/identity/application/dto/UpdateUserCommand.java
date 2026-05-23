package com.lebhas.creativesaas.identity.application.dto;

import com.lebhas.creativesaas.common.security.Role;

import java.util.UUID;

public record UpdateUserCommand(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String phone,
        Role role
) {
}
