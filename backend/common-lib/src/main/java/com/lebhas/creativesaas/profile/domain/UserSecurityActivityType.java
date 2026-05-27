package com.lebhas.creativesaas.profile.domain;

public enum UserSecurityActivityType {
    LOGIN,
    LOGOUT,
    TOKEN_REFRESH,
    PASSWORD_CHANGED,
    PROFILE_UPDATED,
    PROFILE_IMAGE_UPDATED,
    PROFILE_IMAGE_REMOVED,
    ACCOUNT_SETTINGS_UPDATED,
    SESSION_REVOKED,
    SESSIONS_REVOKED,
    ACCOUNT_LOCKED,
    LOGIN_FAILED
}
