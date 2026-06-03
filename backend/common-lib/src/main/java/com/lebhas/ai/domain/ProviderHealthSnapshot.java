package com.lebhas.ai.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "provider_health_snapshots", schema = "platform")
public class ProviderHealthSnapshot extends BaseEntity {

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures;

    @Column(name = "circuit_open", nullable = false)
    private boolean circuitOpen;

    @Column(name = "last_checked_at", nullable = false)
    private Instant lastCheckedAt;

    @Column(name = "failure_reason", length = 240)
    private String failureReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new LinkedHashMap<>();

    protected ProviderHealthSnapshot() {
    }

    public static ProviderHealthSnapshot create(UUID providerId, String status, int consecutiveFailures, boolean circuitOpen, Instant lastCheckedAt, String failureReason, Map<String, Object> metadata) {
        ProviderHealthSnapshot snapshot = new ProviderHealthSnapshot();
        snapshot.providerId = AiToolProvider.require(providerId, "providerId");
        snapshot.status = AiToolProvider.normalizeCode(status, "status");
        snapshot.consecutiveFailures = Math.max(0, consecutiveFailures);
        snapshot.circuitOpen = circuitOpen;
        snapshot.lastCheckedAt = lastCheckedAt == null ? Instant.now() : lastCheckedAt;
        snapshot.failureReason = truncate(AiToolProvider.normalizeNullable(failureReason));
        snapshot.metadata = AiToolProvider.normalizeMetadata(metadata);
        return snapshot;
    }

    public UUID getProviderId() { return providerId; }
    public String getStatus() { return status; }
    public int getConsecutiveFailures() { return consecutiveFailures; }
    public boolean isCircuitOpen() { return circuitOpen; }
    public Instant getLastCheckedAt() { return lastCheckedAt; }
    public String getFailureReason() { return failureReason; }
    public Map<String, Object> getMetadata() { return Map.copyOf(metadata); }

    private static String truncate(String value) {
        return value == null || value.length() <= 240 ? value : value.substring(0, 240);
    }
}
