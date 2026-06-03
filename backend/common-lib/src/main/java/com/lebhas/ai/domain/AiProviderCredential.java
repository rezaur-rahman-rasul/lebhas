package com.lebhas.ai.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "ai_provider_credentials", schema = "platform")
public class AiProviderCredential extends BaseEntity {

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "credential_name", nullable = false, length = 120)
    private String credentialName;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false, length = 20)
    private ProviderEnvironment environment = ProviderEnvironment.SANDBOX;

    @Enumerated(EnumType.STRING)
    @Column(name = "credential_status", nullable = false, length = 30)
    private CredentialStatus credentialStatus = CredentialStatus.NOT_CONFIGURED;

    @Column(name = "encrypted_secret")
    private String encryptedSecret;

    @Column(name = "masked_secret", length = 160)
    private String maskedSecret;

    @Column(name = "webhook_url")
    private String webhookUrl;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_test_status", length = 30)
    private ProviderConnectionTestStatus lastTestStatus;

    @Column(name = "last_tested_at")
    private Instant lastTestedAt;

    @Column(name = "last_test_message")
    private String lastTestMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new LinkedHashMap<>();

    protected AiProviderCredential() {
    }

    public static AiProviderCredential create(UUID providerId, String credentialName, String encryptedSecret, String maskedSecret, boolean active, Map<String, Object> metadata) {
        AiProviderCredential credential = new AiProviderCredential();
        credential.providerId = AiToolProvider.require(providerId, "providerId");
        credential.apply(credentialName, encryptedSecret, maskedSecret, active, metadata);
        return credential;
    }

    public static AiProviderCredential createProviderCredential(
            UUID providerId,
            ProviderEnvironment environment,
            String encryptedSecret,
            String maskedSecret,
            String webhookUrl,
            boolean active,
            Map<String, Object> metadata
    ) {
        AiProviderCredential credential = new AiProviderCredential();
        credential.providerId = AiToolProvider.require(providerId, "providerId");
        credential.applyProviderCredential(environment, encryptedSecret, maskedSecret, webhookUrl, active, metadata);
        return credential;
    }

    public void update(String credentialName, String encryptedSecret, String maskedSecret, boolean active, Map<String, Object> metadata) {
        apply(credentialName, encryptedSecret, maskedSecret, active, metadata);
    }

    public void updateProviderCredential(
            ProviderEnvironment environment,
            String encryptedSecret,
            String maskedSecret,
            String webhookUrl,
            boolean active,
            Map<String, Object> metadata
    ) {
        applyProviderCredential(environment, encryptedSecret, maskedSecret, webhookUrl, active, metadata);
    }

    public void revoke() {
        this.encryptedSecret = null;
        this.maskedSecret = null;
        this.active = false;
        this.credentialStatus = CredentialStatus.REVOKED;
    }

    public void recordTest(ProviderConnectionTestStatus status, Instant testedAt, String message) {
        this.lastTestStatus = status;
        this.lastTestedAt = testedAt;
        this.lastTestMessage = AiToolProvider.normalizeNullable(message);
        if (status == ProviderConnectionTestStatus.FAILED && this.credentialStatus == CredentialStatus.CONFIGURED) {
            this.credentialStatus = CredentialStatus.INVALID;
        }
    }

    public UUID getProviderId() {
        return providerId;
    }

    public String getCredentialName() {
        return credentialName;
    }

    public String getEncryptedSecret() {
        return encryptedSecret;
    }

    public ProviderEnvironment getEnvironment() {
        return environment;
    }

    public CredentialStatus getCredentialStatus() {
        return credentialStatus;
    }

    public String getMaskedSecret() {
        return maskedSecret;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public boolean isActive() {
        return active;
    }

    public ProviderConnectionTestStatus getLastTestStatus() {
        return lastTestStatus;
    }

    public Instant getLastTestedAt() {
        return lastTestedAt;
    }

    public String getLastTestMessage() {
        return lastTestMessage;
    }

    public Map<String, Object> getMetadata() {
        return Map.copyOf(metadata);
    }

    private void apply(String credentialName, String encryptedSecret, String maskedSecret, boolean active, Map<String, Object> metadata) {
        this.credentialName = AiToolProvider.normalizeCode(credentialName, "credentialName");
        this.encryptedSecret = AiToolProvider.normalizeNullable(encryptedSecret);
        this.maskedSecret = AiToolProvider.normalizeNullable(maskedSecret);
        this.active = active;
        this.metadata = AiToolProvider.normalizeMetadata(metadata);
    }

    private void applyProviderCredential(
            ProviderEnvironment environment,
            String encryptedSecret,
            String maskedSecret,
            String webhookUrl,
            boolean active,
            Map<String, Object> metadata
    ) {
        this.environment = environment == null ? ProviderEnvironment.SANDBOX : environment;
        this.credentialName = this.environment.name();
        this.encryptedSecret = AiToolProvider.normalizeNullable(encryptedSecret);
        this.maskedSecret = AiToolProvider.normalizeNullable(maskedSecret);
        this.webhookUrl = AiToolProvider.normalizeNullable(webhookUrl);
        this.active = active;
        this.credentialStatus = this.encryptedSecret == null ? CredentialStatus.NOT_CONFIGURED : CredentialStatus.CONFIGURED;
        this.metadata = AiToolProvider.normalizeMetadata(metadata);
    }
}
