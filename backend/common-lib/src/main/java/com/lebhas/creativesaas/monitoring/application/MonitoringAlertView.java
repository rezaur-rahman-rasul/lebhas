package com.lebhas.creativesaas.monitoring.application;

import com.lebhas.creativesaas.monitoring.domain.MonitoringAlertStatus;
import com.lebhas.creativesaas.monitoring.domain.MonitoringSeverity;
import com.lebhas.creativesaas.monitoring.domain.SystemComponentType;

import java.time.Instant;
import java.util.UUID;

public record MonitoringAlertView(
        UUID id,
        UUID workspaceId,
        String alertKey,
        SystemComponentType componentType,
        String componentName,
        MonitoringSeverity severity,
        MonitoringAlertStatus alertStatus,
        String title,
        String description,
        Instant triggeredAt,
        Instant acknowledgedAt,
        Instant resolvedAt,
        Instant createdAt
) {
}
