package com.lebhas.creativesaas.usage.application.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ShareUsageTrackingCommand(
        String token,
        UUID accessedByUserId,
        String accessIp,
        String userAgent,
        String referrer,
        LocalDate usageMonth
) {
}
