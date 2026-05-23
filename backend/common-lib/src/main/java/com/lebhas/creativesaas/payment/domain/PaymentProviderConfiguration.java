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
        name = "payment_provider_configurations",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payment_provider_config_provider_environment",
                columnNames = {"provider_id", "environment_type"}
        ),
        indexes = {
                @Index(name = "idx_payment_provider_config_provider_id", columnList = "provider_id"),
                @Index(name = "idx_payment_provider_config_environment", columnList = "environment_type"),
                @Index(name = "idx_payment_provider_config_active", columnList = "is_active")
        })
public class PaymentProviderConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment_type", nullable = false, length = 20)
    private PaymentEnvironmentType environmentType;

    @Column(name = "api_base_url", length = 500)
    private String apiBaseUrl;

    @Column(name = "merchant_id", length = 255)
    private String merchantId;

    @Column(name = "encrypted_api_key", length = 2000)
    private String encryptedApiKey;

    @Column(name = "encrypted_secret", length = 2000)
    private String encryptedSecret;

    @Column(name = "encrypted_webhook_secret", length = 2000)
    private String encryptedWebhookSecret;

    @Column(name = "success_url", length = 1000)
    private String successUrl;

    @Column(name = "failure_url", length = 1000)
    private String failureUrl;

    @Column(name = "cancel_url", length = 1000)
    private String cancelUrl;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentProviderConfiguration() {
    }

    public static PaymentProviderConfiguration create(
            UUID providerId,
            PaymentEnvironmentType environmentType,
            String apiBaseUrl,
            String merchantId,
            String encryptedApiKey,
            String encryptedSecret,
            String encryptedWebhookSecret,
            String successUrl,
            String failureUrl,
            String cancelUrl,
            boolean active
    ) {
        PaymentProviderConfiguration configuration = new PaymentProviderConfiguration();
        configuration.providerId = require(providerId, "providerId");
        configuration.environmentType = require(environmentType, "environmentType");
        configuration.apiBaseUrl = normalizeNullable(apiBaseUrl);
        configuration.merchantId = normalizeNullable(merchantId);
        configuration.encryptedApiKey = normalizeNullable(encryptedApiKey);
        configuration.encryptedSecret = normalizeNullable(encryptedSecret);
        configuration.encryptedWebhookSecret = normalizeNullable(encryptedWebhookSecret);
        configuration.successUrl = normalizeNullable(successUrl);
        configuration.failureUrl = normalizeNullable(failureUrl);
        configuration.cancelUrl = normalizeNullable(cancelUrl);
        configuration.active = active;
        return configuration;
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

    public UUID getProviderId() {
        return providerId;
    }

    public PaymentEnvironmentType getEnvironmentType() {
        return environmentType;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getEncryptedApiKey() {
        return encryptedApiKey;
    }

    public String getEncryptedSecret() {
        return encryptedSecret;
    }

    public String getEncryptedWebhookSecret() {
        return encryptedWebhookSecret;
    }

    public String getSuccessUrl() {
        return successUrl;
    }

    public String getFailureUrl() {
        return failureUrl;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public boolean isActive() {
        return active;
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

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
