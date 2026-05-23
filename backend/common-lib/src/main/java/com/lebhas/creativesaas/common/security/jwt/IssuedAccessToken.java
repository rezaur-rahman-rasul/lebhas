package com.lebhas.creativesaas.common.security.jwt;

import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;

import java.time.Instant;
import java.util.Set;

public record IssuedAccessToken(
        String token,
        String tokenId,
        Instant expiresAt,
        Set<Role> roles,
        Set<Permission> permissions
) {
}
