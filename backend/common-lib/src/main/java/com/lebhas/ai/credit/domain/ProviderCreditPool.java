package com.lebhas.ai.credit.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(
        name = "provider_credit_pools",
        schema = "platform",
        indexes = {
                @Index(name = "idx_provider_credit_pools_provider", columnList = "provider_id"),
                @Index(name = "idx_provider_credit_pools_low_balance", columnList = "provider_id,low_balance_threshold")
        })
public class ProviderCreditPool extends BaseEntity {

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "currency", nullable = false, length = 12)
    private String currency;

    @Column(name = "provider_balance_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal providerBalanceAmount;

    @Column(name = "internal_credit_equivalent", nullable = false, precision = 19, scale = 4)
    private BigDecimal internalCreditEquivalent;

    @Column(name = "reserved_internal_credits", nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedInternalCredits;

    @Column(name = "used_internal_credits", nullable = false, precision = 19, scale = 4)
    private BigDecimal usedInternalCredits;

    @Column(name = "low_balance_threshold", nullable = false, precision = 19, scale = 4)
    private BigDecimal lowBalanceThreshold;

    protected ProviderCreditPool() {
    }

    public static ProviderCreditPool create(
            UUID providerId,
            String currency,
            BigDecimal providerBalanceAmount,
            BigDecimal internalCreditEquivalent,
            BigDecimal lowBalanceThreshold
    ) {
        ProviderCreditPool pool = new ProviderCreditPool();
        pool.providerId = require(providerId, "providerId");
        pool.currency = normalizeCurrency(currency);
        pool.providerBalanceAmount = nonNegative(providerBalanceAmount, "providerBalanceAmount");
        pool.internalCreditEquivalent = nonNegative(internalCreditEquivalent, "internalCreditEquivalent");
        pool.reservedInternalCredits = zero();
        pool.usedInternalCredits = zero();
        pool.lowBalanceThreshold = nonNegative(lowBalanceThreshold, "lowBalanceThreshold");
        return pool;
    }

    public void update(String currency, BigDecimal providerBalanceAmount, BigDecimal internalCreditEquivalent, BigDecimal lowBalanceThreshold) {
        BigDecimal normalizedEquivalent = nonNegative(internalCreditEquivalent, "internalCreditEquivalent");
        if (normalizedEquivalent.compareTo(reservedInternalCredits.add(usedInternalCredits)) < 0) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Provider credit pool cannot be lower than already reserved and used credits");
        }
        this.currency = normalizeCurrency(currency);
        this.providerBalanceAmount = nonNegative(providerBalanceAmount, "providerBalanceAmount");
        this.internalCreditEquivalent = normalizedEquivalent;
        this.lowBalanceThreshold = nonNegative(lowBalanceThreshold, "lowBalanceThreshold");
    }

    public BigDecimal adjustInternalEquivalent(BigDecimal amount) {
        BigDecimal normalized = normalize(amount);
        BigDecimal adjusted = internalCreditEquivalent.add(normalized).setScale(4, RoundingMode.HALF_UP);
        if (adjusted.compareTo(reservedInternalCredits.add(usedInternalCredits)) < 0) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Provider credit adjustment cannot make available provider credits negative");
        }
        this.internalCreditEquivalent = adjusted;
        return adjusted;
    }

    public void reserve(BigDecimal amount) {
        BigDecimal normalized = positive(amount, "amount");
        if (availableInternalCredits().compareTo(normalized) < 0) {
            throw new BusinessException(ErrorCode.CREDIT_BALANCE_INSUFFICIENT, "Insufficient provider credit pool balance");
        }
        this.reservedInternalCredits = reservedInternalCredits.add(normalized).setScale(4, RoundingMode.HALF_UP);
    }

    public void useReserved(BigDecimal amount) {
        BigDecimal normalized = positive(amount, "amount");
        if (reservedInternalCredits.compareTo(normalized) < 0) {
            throw new BusinessException(ErrorCode.CREDIT_RESERVE_INVALID, "Provider reserved credits cannot be negative");
        }
        this.reservedInternalCredits = reservedInternalCredits.subtract(normalized).setScale(4, RoundingMode.HALF_UP);
        this.usedInternalCredits = usedInternalCredits.add(normalized).setScale(4, RoundingMode.HALF_UP);
    }

    public void releaseReserved(BigDecimal amount) {
        BigDecimal normalized = positive(amount, "amount");
        if (reservedInternalCredits.compareTo(normalized) < 0) {
            throw new BusinessException(ErrorCode.CREDIT_RESERVE_INVALID, "Provider reserved credits cannot be negative");
        }
        this.reservedInternalCredits = reservedInternalCredits.subtract(normalized).setScale(4, RoundingMode.HALF_UP);
    }

    public void allocateFreeCredit(BigDecimal amount) {
        BigDecimal normalized = positive(amount, "amount");
        if (availableInternalCredits().compareTo(normalized) < 0) {
            throw new BusinessException(ErrorCode.CREDIT_BALANCE_INSUFFICIENT, "Insufficient provider credit pool balance");
        }
        this.usedInternalCredits = usedInternalCredits.add(normalized).setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal availableInternalCredits() {
        return internalCreditEquivalent.subtract(reservedInternalCredits).subtract(usedInternalCredits).setScale(4, RoundingMode.HALF_UP);
    }

    public boolean isLowBalance() {
        return availableInternalCredits().compareTo(lowBalanceThreshold) <= 0;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getProviderBalanceAmount() {
        return providerBalanceAmount;
    }

    public BigDecimal getInternalCreditEquivalent() {
        return internalCreditEquivalent;
    }

    public BigDecimal getReservedInternalCredits() {
        return reservedInternalCredits;
    }

    public BigDecimal getUsedInternalCredits() {
        return usedInternalCredits;
    }

    public BigDecimal getLowBalanceThreshold() {
        return lowBalanceThreshold;
    }

    private static String normalizeCurrency(String value) {
        if (value == null || value.isBlank()) {
            return "USD";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static BigDecimal positive(BigDecimal value, String field) {
        BigDecimal normalized = normalize(value);
        if (normalized.signum() <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return normalized;
    }

    private static BigDecimal nonNegative(BigDecimal value, String field) {
        BigDecimal normalized = normalize(value);
        if (normalized.signum() < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return normalized;
    }

    private static BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            return zero();
        }
        return value.setScale(4, RoundingMode.HALF_UP);
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
