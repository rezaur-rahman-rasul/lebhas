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
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "payment_providers",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(name = "uk_payment_providers_code", columnNames = "code"),
        indexes = {
                @Index(name = "idx_payment_providers_provider_type", columnList = "provider_type"),
                @Index(name = "idx_payment_providers_enabled_priority", columnList = "is_enabled,priority"),
                @Index(name = "idx_payment_providers_sandbox_enabled", columnList = "sandbox_enabled"),
                @Index(name = "idx_payment_providers_live_enabled", columnList = "live_enabled")
        })
public class PaymentProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 80)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 40)
    private PaymentProviderType providerType;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled;

    @Column(name = "sandbox_enabled", nullable = false)
    private boolean sandboxEnabled;

    @Column(name = "live_enabled", nullable = false)
    private boolean liveEnabled;

    @Column(nullable = false)
    private int priority;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentProvider() {
    }

    public static PaymentProvider create(
            String name,
            String code,
            PaymentProviderType providerType,
            boolean enabled,
            boolean sandboxEnabled,
            boolean liveEnabled,
            int priority
    ) {
        PaymentProvider provider = new PaymentProvider();
        provider.name = normalizeRequired(name, "name");
        provider.code = normalizeCode(code);
        provider.providerType = require(providerType, "providerType");
        provider.enabled = enabled;
        provider.sandboxEnabled = sandboxEnabled;
        provider.liveEnabled = liveEnabled;
        provider.priority = Math.max(priority, 0);
        return provider;
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

    public PaymentProviderType getProviderType() {
        return providerType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isSandboxEnabled() {
        return sandboxEnabled;
    }

    public boolean isLiveEnabled() {
        return liveEnabled;
    }

    public int getPriority() {
        return priority;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeCode(String value) {
        return normalizeRequired(value, "code").trim().toUpperCase();
    }
}
