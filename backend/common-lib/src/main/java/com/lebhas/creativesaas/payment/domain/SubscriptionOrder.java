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
        name = "subscription_orders",
        schema = "platform",
        indexes = {
                @Index(name = "idx_subscription_orders_workspace_created_at", columnList = "workspace_id,created_at"),
                @Index(name = "idx_subscription_orders_pricing_plan_id", columnList = "pricing_plan_id"),
                @Index(name = "idx_subscription_orders_requested_by", columnList = "requested_by"),
                @Index(name = "idx_subscription_orders_payment_transaction_id", columnList = "payment_transaction_id"),
                @Index(name = "idx_subscription_orders_status", columnList = "status")
        })
public class SubscriptionOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "pricing_plan_id", nullable = false)
    private UUID pricingPlanId;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    private BillingCycle billingCycle;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentOrderStatus status;

    @Column(name = "payment_transaction_id")
    private UUID paymentTransactionId;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SubscriptionOrder() {
    }

    public static SubscriptionOrder create(
            UUID workspaceId,
            UUID pricingPlanId,
            UUID requestedBy,
            BillingCycle billingCycle,
            BigDecimal amount,
            String currency,
            Instant startsAt,
            Instant expiresAt
    ) {
        SubscriptionOrder order = new SubscriptionOrder();
        order.workspaceId = PaymentTransaction.require(workspaceId, "workspaceId");
        order.pricingPlanId = PaymentTransaction.require(pricingPlanId, "pricingPlanId");
        order.requestedBy = PaymentTransaction.require(requestedBy, "requestedBy");
        order.billingCycle = PaymentTransaction.require(billingCycle, "billingCycle");
        order.amount = PaymentTransaction.normalizeMoney(amount, "amount");
        order.currency = PaymentTransaction.normalizeCurrency(currency);
        order.status = PaymentOrderStatus.CREATED;
        order.startsAt = startsAt;
        order.expiresAt = expiresAt;
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

    public UUID getPricingPlanId() {
        return pricingPlanId;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
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

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
