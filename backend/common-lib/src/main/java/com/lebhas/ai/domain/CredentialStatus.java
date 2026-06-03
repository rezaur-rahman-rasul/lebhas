package com.lebhas.ai.domain;

public enum CredentialStatus {
    NOT_CONFIGURED,
    CONFIGURED,
    ROTATION_REQUIRED,
    TEST_FAILED,
    EXPIRED,
    INVALID,
    REVOKED
}
