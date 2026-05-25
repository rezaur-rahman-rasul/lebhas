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
        name = "monitoring_alerts",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(name = "uk_monitoring_alerts_alert_key", columnNames = "alert_key"),
        indexes = {
                @Index(name = "idx_monitoring_alerts_status_severity", columnList = "alert_status,severity"),
                @Index(name = "idx_monitoring_alerts_component_created_at", columnList = "component_type,triggered_at"),
                @Index(name = "idx_monitoring_alerts_workspace_created_at", columnList = "workspace_id,triggered_at")
        })
public class MonitoringAlert extends BaseEntity {

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(name = "alert_key", nullable = false, updatable = false, length = 160)
    private String alertKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "component_type", nullable = false, length = 40)
    private SystemComponentType componentType;

    @Column(name = "component_name", nullable = false, length = 120)
    private String componentName;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 30)
    private MonitoringSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_status", nullable = false, length = 40)
    private MonitoringAlertStatus alertStatus;

    @Column(name = "title", nullable = false, length = 180)
    private String title;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected MonitoringAlert() {
    }

    public static MonitoringAlert create(
            UUID workspaceId,
            String alertKey,
            SystemComponentType componentType,
            String componentName,
            MonitoringSeverity severity,
            String title,
            String description,
            Instant triggeredAt
    ) {
        MonitoringAlert alert = new MonitoringAlert();
        alert.workspaceId = workspaceId;
        alert.alertKey = normalizeRequired(alertKey, "alertKey");
        alert.componentType = require(componentType, "componentType");
        alert.componentName = normalizeRequired(componentName, "componentName");
        alert.severity = require(severity, "severity");
        alert.alertStatus = MonitoringAlertStatus.OPEN;
        alert.title = normalizeRequired(title, "title");
        alert.description = normalizeRequired(description, "description");
        alert.triggeredAt = triggeredAt == null ? Instant.now() : triggeredAt;
        return alert;
    }

    public void acknowledge(Instant acknowledgedAt) {
        this.alertStatus = MonitoringAlertStatus.ACKNOWLEDGED;
        this.acknowledgedAt = acknowledgedAt == null ? Instant.now() : acknowledgedAt;
    }

    public void resolve(Instant resolvedAt) {
        this.alertStatus = MonitoringAlertStatus.RESOLVED;
        this.resolvedAt = resolvedAt == null ? Instant.now() : resolvedAt;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getAlertKey() {
        return alertKey;
    }

    public SystemComponentType getComponentType() {
        return componentType;
    }

    public String getComponentName() {
        return componentName;
    }

    public MonitoringSeverity getSeverity() {
        return severity;
    }

    public MonitoringAlertStatus getAlertStatus() {
        return alertStatus;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
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
}
