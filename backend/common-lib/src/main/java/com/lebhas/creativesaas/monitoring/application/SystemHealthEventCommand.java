package com.lebhas.creativesaas.monitoring.application;

import com.lebhas.creativesaas.monitoring.domain.MonitoringSeverity;
import com.lebhas.creativesaas.monitoring.domain.SystemComponentType;
import com.lebhas.creativesaas.monitoring.domain.SystemHealthStatus;

import java.time.Instant;
import java.util.UUID;

public record SystemHealthEventCommand(
        UUID workspaceId,
        String sourceEventId,
        SystemComponentType componentType,
        String componentName,
        SystemHealthStatus healthStatus,
        MonitoringSeverity severity,
        String message,
        String detailsJson,
        Instant eventAt
) {
}
