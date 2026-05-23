package com.lebhas.creativesaas.common.security;

public final class AuthorityNames {

    private AuthorityNames() {
    }

    public static String role(Role role) {
        return "ROLE_" + role.name();
    }

    public static String permission(Permission permission) {
        return permission.name();
    }
}
