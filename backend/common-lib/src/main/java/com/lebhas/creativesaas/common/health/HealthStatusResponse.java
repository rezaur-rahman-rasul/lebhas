package com.lebhas.creativesaas.common.health;

import java.time.Instant;

public record HealthStatusResponse(
        String service,
        String status,
        String state,
        Instant timestamp
) {
}
