package com.lebhas.creativesaas.usage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "credit_ledger",
        schema = "platform",
        indexes = {
                @Index(name = "idx_credit_ledger_workspace_created_at", columnList = "workspace_id,created_at"),
                @Index(name = "idx_credit_ledger_creative_request_id", columnList = "creative_request_id"),
                @Index(name = "idx_credit_ledger_generated_version_id", columnList = "generated_version_id"),
                @Index(name = "idx_credit_ledger_generation_job_id", columnList = "generation_job_id"),
                @Index(name = "idx_credit_ledger_reference", columnList = "reference_type,reference_id")
        })
public class CreditLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(name = "creative_request_id")
    private UUID creativeRequestId;

    @Column(name = "generated_version_id")
    private UUID generatedVersionId;

    @Column(name = "generation_job_id")
    private UUID generationJobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 40)
    private CreditLedgerTransactionType transactionType;

    @Column(name = "credits_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal creditsAmount;

    @Column(name = "balance_before_transaction", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceBeforeTransaction;

    @Column(name = "balance_after_transaction", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfterTransaction;

    @Column(name = "reference_type", length = 80)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CreditLedger() {
    }

    public static CreditLedger create(
            UUID workspaceId,
            UUID creativeRequestId,
            UUID generatedVersionId,
            UUID generationJobId,
            CreditLedgerTransactionType transactionType,
            BigDecimal creditsAmount,
            BigDecimal balanceBeforeTransaction,
            BigDecimal balanceAfterTransaction,
            String referenceType,
            UUID referenceId,
            String description,
            UUID createdBy
    ) {
        CreditLedger ledger = new CreditLedger();
        ledger.workspaceId = require(workspaceId, "workspaceId");
        ledger.creativeRequestId = creativeRequestId;
        ledger.generatedVersionId = generatedVersionId;
        ledger.generationJobId = generationJobId;
        ledger.transactionType = require(transactionType, "transactionType");
        ledger.creditsAmount = normalizeMoney(creditsAmount, "creditsAmount");
        ledger.balanceBeforeTransaction = normalizeMoney(balanceBeforeTransaction, "balanceBeforeTransaction");
        ledger.balanceAfterTransaction = normalizeMoney(balanceAfterTransaction, "balanceAfterTransaction");
        ledger.referenceType = normalizeNullable(referenceType);
        ledger.referenceId = referenceId;
        ledger.description = normalizeNullable(description);
        ledger.createdBy = createdBy;
        return ledger;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public UUID getCreativeRequestId() {
        return creativeRequestId;
    }

    public UUID getGeneratedVersionId() {
        return generatedVersionId;
    }

    public UUID getGenerationJobId() {
        return generationJobId;
    }

    public CreditLedgerTransactionType getTransactionType() {
        return transactionType;
    }

    public BigDecimal getCreditsAmount() {
        return creditsAmount;
    }

    public BigDecimal getBalanceBeforeTransaction() {
        return balanceBeforeTransaction;
    }

    public BigDecimal getBalanceAfterTransaction() {
        return balanceAfterTransaction;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public String getDescription() {
        return description;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static BigDecimal normalizeMoney(BigDecimal value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
