package com.lebhas.pricing;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Entity
@Table(name = "pricing_plans", schema = "platform")
public class PricingPlan extends BaseEntity {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "code", nullable = false, length = 60)
    private String code;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "monthly_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal monthlyPrice;

    @Column(name = "yearly_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal yearlyPrice;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "is_default", nullable = false)
    private boolean defaultPlan;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected PricingPlan() {
    }

    public static PricingPlan create(
            String name,
            String code,
            String description,
            BigDecimal monthlyPrice,
            BigDecimal yearlyPrice,
            String currency,
            boolean defaultPlan,
            boolean active,
            int sortOrder
    ) {
        PricingPlan pricingPlan = new PricingPlan();
        pricingPlan.name = normalizeRequired(name, "name");
        pricingPlan.code = normalizeUpperRequired(code, "code");
        pricingPlan.description = normalizeNullable(description);
        pricingPlan.monthlyPrice = normalizeMoney(monthlyPrice, "monthlyPrice");
        pricingPlan.yearlyPrice = normalizeMoney(yearlyPrice, "yearlyPrice");
        pricingPlan.currency = normalizeCurrency(currency);
        pricingPlan.defaultPlan = defaultPlan;
        pricingPlan.active = active;
        pricingPlan.sortOrder = normalizeSortOrder(sortOrder);
        return pricingPlan;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getMonthlyPrice() {
        return monthlyPrice;
    }

    public BigDecimal getYearlyPrice() {
        return yearlyPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public boolean isDefault() {
        return defaultPlan;
    }

    public boolean isActive() {
        return active;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void update(
            String name,
            String code,
            String description,
            BigDecimal monthlyPrice,
            BigDecimal yearlyPrice,
            String currency,
            boolean defaultPlan,
            boolean active,
            int sortOrder
    ) {
        this.name = normalizeRequired(name, "name");
        this.code = normalizeUpperRequired(code, "code");
        this.description = normalizeNullable(description);
        this.monthlyPrice = normalizeMoney(monthlyPrice, "monthlyPrice");
        this.yearlyPrice = normalizeMoney(yearlyPrice, "yearlyPrice");
        this.currency = normalizeCurrency(currency);
        this.defaultPlan = defaultPlan;
        this.active = active;
        this.sortOrder = normalizeSortOrder(sortOrder);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void markDefault(boolean defaultPlan) {
        this.defaultPlan = defaultPlan;
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeUpperRequired(String value, String field) {
        return normalizeRequired(value, field).toUpperCase(Locale.ROOT);
    }

    private static String normalizeCurrency(String value) {
        String normalized = normalizeUpperRequired(value, "currency");
        if (normalized.length() != 3) {
            throw new IllegalArgumentException("currency must be a 3-letter code");
        }
        return normalized;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static BigDecimal normalizeMoney(BigDecimal amount, String field) {
        if (amount == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        BigDecimal normalized = amount.setScale(4, RoundingMode.HALF_UP);
        if (normalized.signum() < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return normalized;
    }

    private static int normalizeSortOrder(int sortOrder) {
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder must not be negative");
        }
        return sortOrder;
    }
}
