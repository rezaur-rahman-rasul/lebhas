package com.lebhas.creativesaas.common.security;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record AuthenticatedPrincipal(
        UUID userId,
        UUID workspaceId,
        String deviceId,
        String email,
        Set<Role> roles,
        Set<Permission> permissions,
        String tokenId,
        Instant expiresAt
) {
}
