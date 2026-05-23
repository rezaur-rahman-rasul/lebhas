package com.lebhas.creativesaas.common.security;

public enum Role {
    MASTER,
    ADMIN,
    CREW;

    public boolean isMaster() {
        return this == MASTER;
    }
}
