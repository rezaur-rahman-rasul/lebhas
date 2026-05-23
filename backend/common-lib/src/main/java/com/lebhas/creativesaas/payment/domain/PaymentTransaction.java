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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "payment_transactions",
        schema = "platform",
        indexes = {
                @Index(name = "idx_payment_transactions_workspace_created_at", columnList = "workspace_id,created_at"),
                @Index(name = "idx_payment_transactions_user_id", columnList = "user_id"),
                @Index(name = "idx_payment_transactions_provider_id", columnList = "provider_id"),
                @Index(name = "idx_payment_transactions_purpose_status", columnList = "payment_purpose,status"),
                @Index(name = "idx_payment_transactions_reference", columnList = "reference_type,reference_id"),
                @Index(name = "idx_payment_transactions_provider_transaction_id", columnList = "provider_transaction_id"),
                @Index(name = "idx_payment_transactions_provider_session_id", columnList = "provider_session_id")
        })
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_purpose", nullable = false, length = 60)
    private PaymentPurpose paymentPurpose;

    @Column(name = "reference_type", nullable = false, length = 80)
    private String referenceType;

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "provider_transaction_id", length = 255)
    private String providerTransactionId;

    @Column(name = "provider_session_id", length = 255)
    private String providerSessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentTransactionStatus status;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "initiated_at", nullable = false)
    private Instant initiatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentTransaction() {
    }

    public static PaymentTransaction create(
            UUID workspaceId,
            UUID userId,
            UUID providerId,
            PaymentPurpose paymentPurpose,
            String referenceType,
            UUID referenceId,
            BigDecimal amount,
            String currency
    ) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.workspaceId = require(workspaceId, "workspaceId");
        transaction.userId = require(userId, "userId");
        transaction.providerId = require(providerId, "providerId");
        transaction.paymentPurpose = require(paymentPurpose, "paymentPurpose");
        transaction.referenceType = normalizeRequired(referenceType, "referenceType");
        transaction.referenceId = require(referenceId, "referenceId");
        transaction.amount = normalizeMoney(amount, "amount");
        transaction.currency = normalizeCurrency(currency);
        transaction.status = PaymentTransactionStatus.INITIATED;
        transaction.initiatedAt = Instant.now();
        return transaction;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.initiatedAt == null) {
            this.initiatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public PaymentPurpose getPaymentPurpose() {
        return paymentPurpose;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getProviderTransactionId() {
        return providerTransactionId;
    }

    public String getProviderSessionId() {
        return providerSessionId;
    }

    public PaymentTransactionStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getInitiatedAt() {
        return initiatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    static BigDecimal normalizeMoney(BigDecimal value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        BigDecimal normalized = value.setScale(4, RoundingMode.HALF_UP);
        if (normalized.signum() < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return normalized;
    }

    static String normalizeCurrency(String value) {
        String normalized = normalizeRequired(value, "currency").toUpperCase();
        if (normalized.length() != 3) {
            throw new IllegalArgumentException("currency must be ISO-4217 length");
        }
        return normalized;
    }

    static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
