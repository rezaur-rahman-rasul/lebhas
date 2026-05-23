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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "credit_purchase_orders",
        schema = "platform",
        indexes = {
                @Index(name = "idx_credit_purchase_orders_workspace_created_at", columnList = "workspace_id,created_at"),
                @Index(name = "idx_credit_purchase_orders_credit_package_id", columnList = "credit_package_id"),
                @Index(name = "idx_credit_purchase_orders_requested_by", columnList = "requested_by"),
                @Index(name = "idx_credit_purchase_orders_payment_transaction_id", columnList = "payment_transaction_id"),
                @Index(name = "idx_credit_purchase_orders_status", columnList = "status")
        })
public class CreditPurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "credit_package_id", nullable = false)
    private UUID creditPackageId;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(nullable = false)
    private long credits;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentOrderStatus status;

    @Column(name = "payment_transaction_id")
    private UUID paymentTransactionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CreditPurchaseOrder() {
    }

    public static CreditPurchaseOrder create(
            UUID workspaceId,
            UUID creditPackageId,
            UUID requestedBy,
            long credits,
            BigDecimal amount,
            String currency
    ) {
        CreditPurchaseOrder order = new CreditPurchaseOrder();
        order.workspaceId = PaymentTransaction.require(workspaceId, "workspaceId");
        order.creditPackageId = PaymentTransaction.require(creditPackageId, "creditPackageId");
        order.requestedBy = PaymentTransaction.require(requestedBy, "requestedBy");
        if (credits < 0) {
            throw new IllegalArgumentException("credits must not be negative");
        }
        order.credits = credits;
        order.amount = PaymentTransaction.normalizeMoney(amount, "amount");
        order.currency = PaymentTransaction.normalizeCurrency(currency);
        order.status = PaymentOrderStatus.CREATED;
        return order;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
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

    public UUID getCreditPackageId() {
        return creditPackageId;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public long getCredits() {
        return credits;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentOrderStatus getStatus() {
        return status;
    }

    public UUID getPaymentTransactionId() {
        return paymentTransactionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
