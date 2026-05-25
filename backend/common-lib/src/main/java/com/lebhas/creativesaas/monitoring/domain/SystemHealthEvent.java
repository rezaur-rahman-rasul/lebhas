package com.lebhas.creativesaas.monitoring.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "system_health_events",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(name = "uk_system_health_events_source_event_id", columnNames = "source_event_id"),
        indexes = {
                @Index(name = "idx_system_health_events_component_created_at", columnList = "component_type,event_at"),
                @Index(name = "idx_system_health_events_status_severity", columnList = "health_status,severity"),
                @Index(name = "idx_system_health_events_workspace_created_at", columnList = "workspace_id,event_at")
        })
public class SystemHealthEvent extends BaseEntity {

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(name = "source_event_id", nullable = false, updatable = false, length = 120)
    private String sourceEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "component_type", nullable = false, length = 40)
    private SystemComponentType componentType;

    @Column(name = "component_name", nullable = false, length = 120)
    private String componentName;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false, length = 40)
    private SystemHealthStatus healthStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 30)
    private MonitoringSeverity severity;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    @Column(name = "event_at", nullable = false)
    private Instant eventAt;

    protected SystemHealthEvent() {
    }

    public static SystemHealthEvent create(
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
        SystemHealthEvent event = new SystemHealthEvent();
        event.workspaceId = workspaceId;
        event.sourceEventId = normalizeRequired(sourceEventId, "sourceEventId");
        event.componentType = require(componentType, "componentType");
        event.componentName = normalizeRequired(componentName, "componentName");
        event.healthStatus = require(healthStatus, "healthStatus");
        event.severity = require(severity, "severity");
        event.message = normalizeRequired(message, "message");
        event.detailsJson = normalizeNullable(detailsJson);
        event.eventAt = eventAt == null ? Instant.now() : eventAt;
        return event;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public SystemComponentType getComponentType() {
        return componentType;
    }

    public String getComponentName() {
        return componentName;
    }

    public SystemHealthStatus getHealthStatus() {
        return healthStatus;
    }

    public MonitoringSeverity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public Instant getEventAt() {
        return eventAt;
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
