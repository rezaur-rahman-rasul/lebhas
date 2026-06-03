package com.lebhas.creativesaas.operations.application.dto;

import com.lebhas.creativesaas.operations.domain.DataIntegrityRunStatus;
import com.lebhas.creativesaas.operations.domain.GoLiveChecklistItemStatus;
import com.lebhas.creativesaas.operations.domain.SmokeTestRunStatus;
import com.lebhas.creativesaas.operations.domain.SystemFeatureToggleKey;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class OperationsViews {
    private OperationsViews() {}

    public record ToggleView(SystemFeatureToggleKey key, boolean enabled, String reason, Instant updatedAt) {}
    public record ToggleUpdateRequest(boolean enabled, String reason) {}
    public record MaintenanceStatusView(boolean maintenanceMode, boolean betaOnlyMode) {}
    public record OverviewView(Map<String, Object> status, Map<SystemFeatureToggleKey, Boolean> toggles) {}
    public record SmokeTestRunView(UUID id, SmokeTestRunStatus status, Instant startedAt, Instant completedAt, Map<String, Object> results) {}
    public record ChecklistCommand(String title, String description, String reason) {}
    public record ChecklistItemView(UUID id, String title, String description, GoLiveChecklistItemStatus status, String blockReason) {}
    public record ReadinessView(boolean ready, long pendingItems, long blockedItems, Map<String, Object> details) {}
    public record DetailedReadinessView(long ready, long needsAttention, long blocked, java.util.List<ReadinessCheckView> checks) {}
    public record ReadinessCheckView(String key, String title, String description, String status, String severity, String relatedRoute, Instant lastCheckedAt) {}
    public record SecurityReadinessView(boolean ready, Map<String, Object> checks) {}
    public record DataIntegrityRunView(UUID id, DataIntegrityRunStatus status, Instant startedAt, Instant completedAt, long issueCount, Map<String, Object> results) {}
    public record DependencyHealthView(String status, Map<String, Object> dependencies) {}
}
