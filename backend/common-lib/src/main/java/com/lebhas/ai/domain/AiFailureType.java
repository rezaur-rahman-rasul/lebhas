package com.lebhas.ai.domain;

public enum AiFailureType {
    TIMEOUT,
    RATE_LIMIT,
    PROVIDER_DOWN,
    INVALID_RESPONSE,
    QUALITY_FAILURE,
    COST_LIMIT_EXCEEDED,
    UNKNOWN
}
