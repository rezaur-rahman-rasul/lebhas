package com.lebhas.ai.credit.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Table(
        name = "provider_credit_exchange_policies",
        schema = "platform",
        indexes = {
                @Index(name = "idx_provider_exchange_policy_provider", columnList = "provider_id"),
                @Index(name = "idx_provider_exchange_policy_active", columnList = "provider_id,active")
        })
public class ProviderCreditExchangePolicy extends BaseEntity {

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "internal_credit_per_provider_unit", nullable = false, precision = 19, scale = 4)
    private BigDecimal internalCreditPerProviderUnit;

    @Column(name = "free_signup_credit_percentage", nullable = false, precision = 9, scale = 4)
    private BigDecimal freeSignupCreditPercentage;

    @Column(name = "free_signup_credit_enabled", nullable = false)
    private boolean freeSignupCreditEnabled;

    @Column(name = "max_free_signup_credits", nullable = false, precision = 19, scale = 4)
    private BigDecimal maxFreeSignupCredits;

    @Column(name = "min_provider_balance_required", nullable = false, precision = 19, scale = 4)
    private BigDecimal minProviderBalanceRequired;

    @Column(name = "fallback_free_credits", nullable = false, precision = 19, scale = 4)
    private BigDecimal fallbackFreeCredits;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected ProviderCreditExchangePolicy() {
    }

    public static ProviderCreditExchangePolicy create(
            UUID providerId,
            BigDecimal internalCreditPerProviderUnit,
            BigDecimal freeSignupCreditPercentage,
            boolean freeSignupCreditEnabled,
            BigDecimal maxFreeSignupCredits,
            BigDecimal minProviderBalanceRequired,
            BigDecimal fallbackFreeCredits,
            boolean active
    ) {
        ProviderCreditExchangePolicy policy = new ProviderCreditExchangePolicy();
        policy.providerId = require(providerId, "providerId");
        policy.update(internalCreditPerProviderUnit, freeSignupCreditPercentage, freeSignupCreditEnabled,
                maxFreeSignupCredits, minProviderBalanceRequired, fallbackFreeCredits, active);
        return policy;
    }

    public void update(
            BigDecimal internalCreditPerProviderUnit,
            BigDecimal freeSignupCreditPercentage,
            boolean freeSignupCreditEnabled,
            BigDecimal maxFreeSignupCredits,
            BigDecimal minProviderBalanceRequired,
            BigDecimal fallbackFreeCredits,
            boolean active
    ) {
        this.internalCreditPerProviderUnit = positive(internalCreditPerProviderUnit, "internalCreditPerProviderUnit");
        this.freeSignupCreditPercentage = nonNegative(freeSignupCreditPercentage, "freeSignupCreditPercentage");
        this.freeSignupCreditEnabled = freeSignupCreditEnabled;
        this.maxFreeSignupCredits = nonNegative(maxFreeSignupCredits, "maxFreeSignupCredits");
        this.minProviderBalanceRequired = nonNegative(minProviderBalanceRequired, "minProviderBalanceRequired");
        this.fallbackFreeCredits = nonNegative(fallbackFreeCredits, "fallbackFreeCredits");
        this.active = active;
    }

    public BigDecimal calculateFreeSignupCredits(BigDecimal availableProviderEquivalent) {
        BigDecimal available = nonNegative(availableProviderEquivalent, "availableProviderEquivalent");
        if (!freeSignupCreditEnabled) {
            return zero();
        }
        if (available.compareTo(minProviderBalanceRequired) < 0) {
            return fallbackFreeCredits;
        }
        BigDecimal calculated = available.multiply(freeSignupCreditPercentage)
                .divide(new BigDecimal("100.0000"), 4, RoundingMode.HALF_UP);
        if (maxFreeSignupCredits.signum() > 0 && calculated.compareTo(maxFreeSignupCredits) > 0) {
            return maxFreeSignupCredits;
        }
        return calculated.setScale(4, RoundingMode.HALF_UP);
    }

    public UUID getProviderId() {
        return providerId;
    }

    public BigDecimal getInternalCreditPerProviderUnit() {
        return internalCreditPerProviderUnit;
    }

    public BigDecimal getFreeSignupCreditPercentage() {
        return freeSignupCreditPercentage;
    }

    public boolean isFreeSignupCreditEnabled() {
        return freeSignupCreditEnabled;
    }

    public BigDecimal getMaxFreeSignupCredits() {
        return maxFreeSignupCredits;
    }

    public BigDecimal getMinProviderBalanceRequired() {
        return minProviderBalanceRequired;
    }

    public BigDecimal getFallbackFreeCredits() {
        return fallbackFreeCredits;
    }

    public boolean isActive() {
        return active;
    }

    private static BigDecimal positive(BigDecimal value, String field) {
        BigDecimal normalized = nonNegative(value, field);
        if (normalized.signum() <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return normalized;
    }

    private static BigDecimal nonNegative(BigDecimal value, String field) {
        BigDecimal normalized = value == null ? zero() : value.setScale(4, RoundingMode.HALF_UP);
        if (normalized.signum() < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return normalized;
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }
}
