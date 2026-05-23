package com.lebhas.creativesaas.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "credit_packages",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(name = "uk_credit_packages_code", columnNames = "code"),
        indexes = {
                @Index(name = "idx_credit_packages_active_sort", columnList = "is_active,sort_order"),
                @Index(name = "idx_credit_packages_currency", columnList = "currency")
        })
public class CreditPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 80)
    private String code;

    @Column(nullable = false)
    private long credits;

    @Column(name = "bonus_credits", nullable = false)
    private long bonusCredits;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CreditPackage() {
    }

    public static CreditPackage create(
            String name,
            String code,
            long credits,
            long bonusCredits,
            BigDecimal price,
            String currency,
            boolean active,
            int sortOrder
    ) {
        CreditPackage creditPackage = new CreditPackage();
        creditPackage.name = PaymentTransaction.normalizeRequired(name, "name");
        creditPackage.code = PaymentTransaction.normalizeRequired(code, "code").toUpperCase();
        creditPackage.credits = requireNonNegative(credits, "credits");
        creditPackage.bonusCredits = requireNonNegative(bonusCredits, "bonusCredits");
        creditPackage.price = PaymentTransaction.normalizeMoney(price, "price");
        creditPackage.currency = PaymentTransaction.normalizeCurrency(currency);
        creditPackage.active = active;
        creditPackage.sortOrder = Math.max(sortOrder, 0);
        return creditPackage;
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

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public long getCredits() {
        return credits;
    }

    public long getBonusCredits() {
        return bonusCredits;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public boolean isActive() {
        return active;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static long requireNonNegative(long value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return value;
    }
}
