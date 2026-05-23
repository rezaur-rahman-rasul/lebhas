package com.lebhas.ai.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "ai_failure_logs", schema = "platform")
public class AiFailureLog extends BaseEntity {

    @Column(name = "creative_request_id", nullable = false)
    private UUID creativeRequestId;

    @Column(name = "layer_id", nullable = false)
    private UUID layerId;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "model_name", nullable = false, length = 160)
    private String modelName;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_type", nullable = false, length = 40)
    private AiFailureType failureType;

    @Column(name = "failure_reason", nullable = false, columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "retry_attempt", nullable = false)
    private int retryAttempt;

    @Column(name = "fallback_triggered", nullable = false)
    private boolean fallbackTriggered;

    protected AiFailureLog() {
    }

    public static AiFailureLog create(
            UUID creativeRequestId,
            UUID layerId,
            UUID providerId,
            String modelName,
            AiFailureType failureType,
            String failureReason,
            int retryAttempt,
            boolean fallbackTriggered
    ) {
        AiFailureLog log = new AiFailureLog();
        log.creativeRequestId = AiToolProvider.require(creativeRequestId, "creativeRequestId");
        log.layerId = AiToolProvider.require(layerId, "layerId");
        log.providerId = AiToolProvider.require(providerId, "providerId");
        log.modelName = AiToolProvider.normalizeRequired(modelName, "modelName");
        log.failureType = failureType == null ? AiFailureType.UNKNOWN : failureType;
        log.failureReason = AiToolProvider.normalizeRequired(failureReason, "failureReason");
        log.retryAttempt = (int) AiProviderMetrics.nonNegative(retryAttempt, "retryAttempt");
        log.fallbackTriggered = fallbackTriggered;
        return log;
    }

    public UUID getCreativeRequestId() {
        return creativeRequestId;
    }

    public UUID getLayerId() {
        return layerId;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public String getModelName() {
        return modelName;
    }

    public AiFailureType getFailureType() {
        return failureType;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public int getRetryAttempt() {
        return retryAttempt;
    }

    public boolean isFallbackTriggered() {
        return fallbackTriggered;
    }
}
