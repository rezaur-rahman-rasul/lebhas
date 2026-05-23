package com.lebhas.creativesaas.identity.application.dto;

import java.util.UUID;

public record RegisterUserCommand(
        String firstName,
        String lastName,
        String email,
        String phone,
        String password,
        UUID workspaceId,
        String invitationToken
) {
}
