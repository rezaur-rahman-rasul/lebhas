package com.lebhas.ai.application.dto;

import java.time.Instant;
import java.util.Map;

public record SmsProviderActionResult(
        String providerCode,
        boolean success,
        String action,
        Integer httpStatus,
        String message,
        String safeEndpoint,
        Map<String, Object> response,
        Instant testedAt
) {
}
