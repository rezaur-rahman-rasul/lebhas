package com.lebhas.creativesaas.monitoring.application;

import com.lebhas.creativesaas.monitoring.domain.MonitoringSeverity;
import com.lebhas.creativesaas.monitoring.domain.SystemComponentType;

import java.time.Instant;
import java.util.UUID;

public record MonitoringAlertCommand(
        UUID workspaceId,
        String alertKey,
        SystemComponentType componentType,
        String componentName,
        MonitoringSeverity severity,
        String title,
        String description,
        Instant triggeredAt
) {
}
