package com.lebhas.creativesaas.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "payment_webhook_logs",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payment_webhook_logs_signature",
                columnNames = {"provider_id", "signature_hash"}
        ),
        indexes = {
                @Index(name = "idx_payment_webhook_logs_provider_created_at", columnList = "provider_id,created_at"),
                @Index(name = "idx_payment_webhook_logs_provider_transaction_id", columnList = "provider_transaction_id"),
                @Index(name = "idx_payment_webhook_logs_event_type", columnList = "webhook_event_type"),
                @Index(name = "idx_payment_webhook_logs_processed", columnList = "processed"),
                @Index(name = "idx_payment_webhook_logs_verification_status", columnList = "verification_status")
        })
public class PaymentWebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "provider_transaction_id", length = 255)
    private String providerTransactionId;

    @Column(name = "webhook_event_type", nullable = false, length = 120)
    private String webhookEventType;

    @Column(name = "request_payload", nullable = false, columnDefinition = "TEXT")
    private String requestPayload;

    @Column(name = "signature_hash", nullable = false, length = 255)
    private String signatureHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private PaymentWebhookVerificationStatus verificationStatus;

    @Column(nullable = false)
    private boolean processed;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected PaymentWebhookLog() {
    }

    public static PaymentWebhookLog create(
            UUID providerId,
            String providerTransactionId,
            String webhookEventType,
            String requestPayload,
            String signatureHash,
            PaymentWebhookVerificationStatus verificationStatus,
            boolean processed,
            String failureReason
    ) {
        PaymentWebhookLog log = new PaymentWebhookLog();
        log.providerId = PaymentTransaction.require(providerId, "providerId");
        log.providerTransactionId = PaymentTransaction.normalizeNullable(providerTransactionId);
        log.webhookEventType = PaymentTransaction.normalizeRequired(webhookEventType, "webhookEventType");
        log.requestPayload = PaymentTransaction.normalizeRequired(requestPayload, "requestPayload");
        log.signatureHash = PaymentTransaction.normalizeRequired(signatureHash, "signatureHash");
        log.verificationStatus = verificationStatus == null ? PaymentWebhookVerificationStatus.PENDING : verificationStatus;
        log.processed = processed;
        log.failureReason = PaymentTransaction.normalizeNullable(failureReason);
        return log;
    }

    public void markVerification(PaymentWebhookVerificationStatus verificationStatus, String providerTransactionId, String webhookEventType, String failureReason) {
        this.verificationStatus = verificationStatus == null ? PaymentWebhookVerificationStatus.PENDING : verificationStatus;
        this.providerTransactionId = PaymentTransaction.normalizeNullable(providerTransactionId);
        if (webhookEventType != null && !webhookEventType.isBlank()) {
            this.webhookEventType = PaymentTransaction.normalizeRequired(webhookEventType, "webhookEventType");
        }
        this.failureReason = PaymentTransaction.normalizeNullable(failureReason);
    }

    public void markProcessed() {
        this.processed = true;
        this.processedAt = Instant.now();
    }

    public void markProcessedFailure(String failureReason) {
        this.processed = true;
        this.failureReason = PaymentTransaction.normalizeNullable(failureReason);
        this.processedAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        if (this.processed && this.processedAt == null) {
            this.processedAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public String getProviderTransactionId() {
        return providerTransactionId;
    }

    public String getWebhookEventType() {
        return webhookEventType;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public String getSignatureHash() {
        return signatureHash;
    }

    public PaymentWebhookVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public boolean isProcessed() {
        return processed;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
