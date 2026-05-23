package com.lebhas.creativesaas.generation.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "generation_jobs", schema = "platform")
public class GenerationJobEntity extends TenantAwareEntity {

    @Column(name = "creative_request_id", nullable = false, updatable = false)
    private UUID creativeRequestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creative_request_id", nullable = false, insertable = false, updatable = false)
    private CreativeRequestEntity creativeRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private GenerationJobStatus status;

    @Column(name = "provider", length = 120)
    private String provider;

    @Column(name = "model", length = 160)
    private String model;

    @Column(name = "queued_at", nullable = false, updatable = false)
    private Instant queuedAt;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "request_id", updatable = false)
    private UUID requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", length = 40)
    private GenerationJobType jobType;

    @Column(name = "provider_job_id", length = 160)
    private String providerJobId;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts")
    private int maxAttempts;

    @Column(name = "queue_name", length = 160)
    private String queueName;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    protected GenerationJobEntity() {
    }

    public static GenerationJobEntity create(
            UUID workspaceId,
            UUID creativeRequestId,
            String provider,
            String model
    ) {
        return create(workspaceId, creativeRequestId, GenerationJobStatus.QUEUED, provider, model);
    }

    public static GenerationJobEntity create(
            UUID workspaceId,
            UUID creativeRequestId,
            GenerationJobStatus status,
            String provider,
            String model
    ) {
        GenerationJobEntity job = new GenerationJobEntity();
        job.assignWorkspace(workspaceId);
        job.creativeRequestId = require(creativeRequestId, "creativeRequestId");
        job.status = status == null ? GenerationJobStatus.QUEUED : status;
        job.provider = normalizeNullable(provider);
        job.model = normalizeNullable(model);
        job.queuedAt = Instant.now();
        job.retryCount = 0;
        job.attemptCount = 0;
        return job;
    }

    public static GenerationJobEntity queue(
            UUID workspaceId,
            UUID requestId,
            GenerationJobType jobType,
            String queueName,
            int maxAttempts
    ) {
        GenerationJobEntity job = create(workspaceId, requestId, null, null);
        job.requestId = requestId;
        job.jobType = jobType;
        job.queueName = normalizeNullable(queueName);
        job.maxAttempts = Math.max(1, maxAttempts);
        return job;
    }

    public UUID getCreativeRequestId() {
        return creativeRequestId;
    }

    public CreativeRequestEntity getCreativeRequest() {
        return creativeRequest;
    }

    public GenerationJobStatus getJobStatus() {
        return status;
    }

    public CreativeGenerationStatus getStatus() {
        return status == null ? null : CreativeGenerationStatus.valueOf(status.name());
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public Instant getQueuedAt() {
        return queuedAt;
    }

    public Instant getProcessingStartedAt() {
        return processingStartedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public UUID getRequestId() {
        return requestId == null ? creativeRequestId : requestId;
    }

    public GenerationJobType getJobType() {
        return jobType;
    }

    public String getProviderJobId() {
        return providerJobId;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public String getQueueName() {
        return queueName;
    }

    public Instant getStartedAt() {
        return processingStartedAt == null ? startedAt : processingStartedAt;
    }

    public String getErrorMessage() {
        return failureReason == null ? errorMessage : failureReason;
    }

    public void markStarted() {
        this.status = GenerationJobStatus.PROCESSING;
        this.attemptCount += 1;
        this.processingStartedAt = Instant.now();
        this.startedAt = this.processingStartedAt;
        this.failureReason = null;
        this.errorMessage = null;
    }

    public void markQueuedForRetry(String failureReason) {
        this.status = GenerationJobStatus.QUEUED;
        this.retryCount += 1;
        this.failureReason = truncate(failureReason);
        this.errorMessage = truncate(failureReason);
    }

    public void markCompleted(String providerJobId) {
        markCompleted(this.provider, this.model, providerJobId);
    }

    public void markCompleted(String provider, String model, String providerJobId) {
        this.status = GenerationJobStatus.COMPLETED;
        this.provider = normalizeNullable(provider);
        this.model = normalizeNullable(model);
        this.providerJobId = normalizeNullable(providerJobId);
        this.completedAt = Instant.now();
        this.failedAt = null;
        this.failureReason = null;
        this.errorMessage = null;
    }

    public void markFailed(String failureReason) {
        this.status = GenerationJobStatus.FAILED;
        this.failedAt = Instant.now();
        this.failureReason = truncate(failureReason);
        this.errorMessage = truncate(failureReason);
    }

    public void markCancelled(String reason) {
        this.status = GenerationJobStatus.CANCELLED;
        this.failedAt = Instant.now();
        this.failureReason = truncate(reason);
        this.errorMessage = truncate(reason);
    }

    public boolean canRetry() {
        return maxAttempts <= 0 || attemptCount < maxAttempts;
    }

    public boolean isTerminal() {
        return status == GenerationJobStatus.COMPLETED
                || status == GenerationJobStatus.FAILED
                || status == GenerationJobStatus.CANCELLED;
    }

    private static UUID require(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 1000 ? normalized : normalized.substring(0, 1000);
    }
}
