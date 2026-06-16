package com.lebhas.ai.credit.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Entity
@Table(
        name = "credit_value_policies",
        schema = "platform",
        indexes = {
                @Index(name = "idx_credit_value_policy_active", columnList = "active"),
                @Index(name = "idx_credit_value_policy_effective", columnList = "effective_from")
        })
public class CreditValuePolicy extends BaseEntity {

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "credit_usd_value", nullable = false, precision = 18, scale = 6)
    private BigDecimal creditUsdValue;

    @Column(name = "average_provider_cost_per_creative_usd", nullable = false, precision = 18, scale = 6)
    private BigDecimal averageProviderCostPerCreativeUsd;

    @Column(name = "provider_cost_multiplier", nullable = false, precision = 12, scale = 4)
    private BigDecimal providerCostMultiplier;

    @Column(name = "free_signup_credit_enabled", nullable = false)
    private boolean freeSignupCreditEnabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "free_signup_mode", nullable = false, length = 50)
    private FreeSignupCreditMode freeSignupMode;

    @Column(name = "free_signup_credits", nullable = false, precision = 18, scale = 4)
    private BigDecimal freeSignupCredits;

    @Column(name = "free_signup_usd_value", nullable = false, precision = 18, scale = 6)
    private BigDecimal freeSignupUsdValue;

    @Column(name = "free_signup_percentage", nullable = false, precision = 8, scale = 4)
    private BigDecimal freeSignupPercentage;

    @Column(name = "one_time_per_workspace", nullable = false)
    private boolean oneTimePerWorkspace;

    @Column(name = "minimum_wallet_balance_warning", nullable = false, precision = 18, scale = 4)
    private BigDecimal minimumWalletBalanceWarning;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    protected CreditValuePolicy() {
    }

    public static CreditValuePolicy create(
            String currency,
            BigDecimal creditUsdValue,
            BigDecimal averageProviderCostPerCreativeUsd,
            BigDecimal providerCostMultiplier,
            boolean freeSignupCreditEnabled,
            FreeSignupCreditMode freeSignupMode,
            BigDecimal freeSignupCredits,
            BigDecimal freeSignupUsdValue,
            BigDecimal freeSignupPercentage,
            boolean oneTimePerWorkspace,
            BigDecimal minimumWalletBalanceWarning,
            boolean active,
            Instant effectiveFrom
    ) {
        CreditValuePolicy policy = new CreditValuePolicy();
        policy.update(currency, creditUsdValue, averageProviderCostPerCreativeUsd, providerCostMultiplier,
                freeSignupCreditEnabled, freeSignupMode, freeSignupCredits, freeSignupUsdValue, freeSignupPercentage,
                oneTimePerWorkspace, minimumWalletBalanceWarning, active, effectiveFrom);
        return policy;
    }

    public void update(
            String currency,
            BigDecimal creditUsdValue,
            BigDecimal averageProviderCostPerCreativeUsd,
            BigDecimal providerCostMultiplier,
            boolean freeSignupCreditEnabled,
            FreeSignupCreditMode freeSignupMode,
            BigDecimal freeSignupCredits,
            BigDecimal freeSignupUsdValue,
            BigDecimal freeSignupPercentage,
            boolean oneTimePerWorkspace,
            BigDecimal minimumWalletBalanceWarning,
            boolean active,
            Instant effectiveFrom
    ) {
        this.currency = normalizeCurrency(currency);
        this.creditUsdValue = positive(creditUsdValue, "creditUsdValue", 6);
        this.averageProviderCostPerCreativeUsd = nonNegative(averageProviderCostPerCreativeUsd, "averageProviderCostPerCreativeUsd", 6);
        this.providerCostMultiplier = minimum(providerCostMultiplier, "providerCostMultiplier", BigDecimal.ONE, 4);
        this.freeSignupCreditEnabled = freeSignupCreditEnabled;
        this.freeSignupMode = freeSignupMode == null ? FreeSignupCreditMode.FIXED_CREDITS : freeSignupMode;
        this.freeSignupCredits = nonNegative(freeSignupCredits, "freeSignupCredits", 4);
        this.freeSignupUsdValue = nonNegative(freeSignupUsdValue, "freeSignupUsdValue", 6);
        this.freeSignupPercentage = percentage(freeSignupPercentage);
        this.oneTimePerWorkspace = oneTimePerWorkspace;
        this.minimumWalletBalanceWarning = nonNegative(minimumWalletBalanceWarning, "minimumWalletBalanceWarning", 4);
        this.active = active;
        this.effectiveFrom = effectiveFrom;
    }

    public BigDecimal calculatedCreativeCostUsd(BigDecimal providerCostUsd) {
        return providerCost(providerCostUsd).multiply(providerCostMultiplier).setScale(6, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateCreativeCreditCost(BigDecimal providerCostUsd, int versions) {
        BigDecimal credits = calculatedCreativeCostUsd(providerCostUsd)
                .divide(creditUsdValue, 0, RoundingMode.CEILING);
        return credits.multiply(BigDecimal.valueOf(Math.max(1, versions))).setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateFreeSignupCredits(BigDecimal providerAvailableInternalEquivalentCredits) {
        if (!freeSignupCreditEnabled) {
            return zero4();
        }
        return switch (freeSignupMode) {
            case FIXED_CREDITS -> freeSignupCredits.setScale(4, RoundingMode.HALF_UP);
            case FIXED_USD_VALUE -> freeSignupUsdValue.divide(creditUsdValue, 0, RoundingMode.FLOOR).setScale(4, RoundingMode.HALF_UP);
            case PERCENTAGE_OF_PROVIDER_POOL -> nonNegative(providerAvailableInternalEquivalentCredits, "providerAvailableInternalEquivalentCredits", 4)
                    .multiply(freeSignupPercentage)
                    .divide(new BigDecimal("100.0000"), 4, RoundingMode.FLOOR);
        };
    }

    public BigDecimal freeSignupUsdEquivalent(BigDecimal freeSignupCredits) {
        return nonNegative(freeSignupCredits, "freeSignupCredits", 4)
                .multiply(creditUsdValue)
                .setScale(6, RoundingMode.HALF_UP);
    }

    public BigDecimal providerCost(BigDecimal providerCostUsd) {
        BigDecimal cost = providerCostUsd == null ? averageProviderCostPerCreativeUsd : providerCostUsd;
        return nonNegative(cost, "providerCostUsd", 6);
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getCreditUsdValue() {
        return creditUsdValue;
    }

    public BigDecimal getAverageProviderCostPerCreativeUsd() {
        return averageProviderCostPerCreativeUsd;
    }

    public BigDecimal getProviderCostMultiplier() {
        return providerCostMultiplier;
    }

    public boolean isFreeSignupCreditEnabled() {
        return freeSignupCreditEnabled;
    }

    public FreeSignupCreditMode getFreeSignupMode() {
        return freeSignupMode;
    }

    public BigDecimal getFreeSignupCredits() {
        return freeSignupCredits;
    }

    public BigDecimal getFreeSignupUsdValue() {
        return freeSignupUsdValue;
    }

    public BigDecimal getFreeSignupPercentage() {
        return freeSignupPercentage;
    }

    public boolean isOneTimePerWorkspace() {
        return oneTimePerWorkspace;
    }

    public BigDecimal getMinimumWalletBalanceWarning() {
        return minimumWalletBalanceWarning;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    private static String normalizeCurrency(String value) {
        if (value == null || value.isBlank()) {
            return "USD";
        }
        return value.trim().toUpperCase();
    }

    private static BigDecimal percentage(BigDecimal value) {
        BigDecimal normalized = nonNegative(value, "freeSignupPercentage", 4);
        if (normalized.compareTo(new BigDecimal("100.0000")) > 0) {
            throw new IllegalArgumentException("freeSignupPercentage must be between 0 and 100");
        }
        return normalized;
    }

    private static BigDecimal minimum(BigDecimal value, String field, BigDecimal minimum, int scale) {
        BigDecimal normalized = nonNegative(value, field, scale);
        if (normalized.compareTo(minimum.setScale(scale, RoundingMode.HALF_UP)) < 0) {
            throw new IllegalArgumentException(field + " must be at least " + minimum);
        }
        return normalized;
    }

    private static BigDecimal positive(BigDecimal value, String field, int scale) {
        BigDecimal normalized = nonNegative(value, field, scale);
        if (normalized.signum() <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return normalized;
    }

    private static BigDecimal nonNegative(BigDecimal value, String field, int scale) {
        BigDecimal normalized = value == null ? BigDecimal.ZERO : value;
        normalized = normalized.setScale(scale, RoundingMode.HALF_UP);
        if (normalized.signum() < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return normalized;
    }

    private static BigDecimal zero4() {
        return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    }
}
