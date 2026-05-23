package com.lebhas.creativesaas.credit.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Table(name = "credit_transactions", schema = "platform")
public class CreditTransactionEntity extends TenantAwareEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private CreditTransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "reference_type", length = 80)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CreditTransactionStatus status;

    protected CreditTransactionEntity() {
    }

    public static CreditTransactionEntity create(
            UUID workspaceId,
            CreditTransactionType transactionType,
            BigDecimal amount,
            String referenceType,
            UUID referenceId,
            CreditTransactionStatus status
    ) {
        CreditTransactionEntity entity = new CreditTransactionEntity();
        entity.assignWorkspace(workspaceId);
        entity.transactionType = transactionType;
        entity.amount = amount.setScale(4, RoundingMode.HALF_UP);
        entity.referenceType = referenceType == null ? null : referenceType.trim();
        entity.referenceId = referenceId;
        entity.status = status == null ? CreditTransactionStatus.COMPLETED : status;
        return entity;
    }

    public CreditTransactionType getTransactionType() {
        return transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public CreditTransactionStatus getStatus() {
        return status;
    }
}
