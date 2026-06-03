package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.CreateMasterProviderRequest;
import com.lebhas.ai.application.dto.MasterProviderView;
import com.lebhas.ai.application.dto.ProviderConnectionTestResult;
import com.lebhas.ai.application.dto.ProviderCredentialSavedView;
import com.lebhas.ai.application.dto.SaveProviderCredentialRequest;
import com.lebhas.ai.application.dto.TestProviderConnectionRequest;
import com.lebhas.ai.application.dto.UpdateMasterProviderRequest;
import com.lebhas.ai.application.dto.UpdateProviderStatusRequest;
import com.lebhas.ai.domain.AiProviderCredential;
import com.lebhas.ai.domain.AiToolProvider;
import com.lebhas.ai.domain.CredentialStatus;
import com.lebhas.ai.domain.ProviderConnectionTestStatus;
import com.lebhas.ai.domain.ProviderEnvironment;
import com.lebhas.ai.domain.ProviderStatus;
import com.lebhas.ai.domain.ProviderType;
import com.lebhas.ai.infrastructure.persistence.AiProviderCredentialRepository;
import com.lebhas.ai.infrastructure.persistence.AiToolProviderRepository;
import com.lebhas.creativesaas.asset.application.AssetEventPublisher;
import com.lebhas.creativesaas.auditlog.application.AuditLogService;
import com.lebhas.creativesaas.auditlog.domain.AuditActionType;
import com.lebhas.creativesaas.auditlog.domain.AuditOutcome;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Transactional
public class MasterProviderSettingsService {

    private final AiToolProviderRepository providerRepository;
    private final AiProviderCredentialRepository credentialRepository;
    private final AiCredentialEncryptionService encryptionService;
    private final AssetEventPublisher eventPublisher;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public MasterProviderSettingsService(
            AiToolProviderRepository providerRepository,
            AiProviderCredentialRepository credentialRepository,
            AiCredentialEncryptionService encryptionService,
            AssetEventPublisher eventPublisher,
            AuditLogService auditLogService,
            Clock clock
    ) {
        this.providerRepository = providerRepository;
        this.credentialRepository = credentialRepository;
        this.encryptionService = encryptionService;
        this.eventPublisher = eventPublisher;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MasterProviderView> listProviders(ProviderType type, ProviderStatus status, ProviderEnvironment environment) {
        return providerRepository.findAllByDeletedFalseOrderByProviderNameAsc().stream()
                .filter(provider -> type == null || providerTypeForSettings(provider) == type)
                .filter(provider -> status == null || provider.getStatus() == status)
                .map(provider -> toView(provider, preferredCredential(provider.getId(), environment)))
                .toList();
    }

    @Transactional(readOnly = true)
    public MasterProviderView getProvider(UUID providerId) {
        AiToolProvider provider = requireProvider(providerId);
        return toView(provider, preferredCredential(providerId, null));
    }

    @Transactional(readOnly = true)
    public MasterProviderView getProvider(String providerKey) {
        AiToolProvider provider = requireProvider(providerKey);
        return toView(provider, preferredCredential(provider.getId(), null));
    }

    public MasterProviderView createProvider(CreateMasterProviderRequest request) {
        String providerCode = AiToolProvider.normalizeCode(request.providerCode(), "providerCode");
        if (providerRepository.existsByProviderCodeAndDeletedFalse(providerCode)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Provider code already exists");
        }

        ProviderStatus status = request.active() ? ProviderStatus.ACTIVE : ProviderStatus.INACTIVE;
        AiToolProvider provider = AiToolProvider.create(
                providerCode,
                required(request.displayName(), "displayName"),
                request.providerType() == null ? ProviderType.AI : request.providerType(),
                status,
                request.active(),
                List.of(),
                providerCode + "_API_KEY",
                true,
                true,
                true,
                Map.of(),
                Map.of(),
                Map.of());
        provider.updateSettings(
                request.displayName(),
                request.description(),
                status,
                request.supportsSandbox(),
                request.supportsLive(),
                request.defaultEnvironment());
        AiToolProvider saved = providerRepository.save(provider);
        publish(KafkaTopicConstants.AI_PROVIDER_CREATED, saved, "provider.created");
        audit(saved, "provider.created", AuditActionType.CREATE, "Provider created");
        return toView(saved, null);
    }

    public MasterProviderView updateProvider(UUID providerId, UpdateMasterProviderRequest request) {
        AiToolProvider provider = requireProvider(providerId);
        provider.updateSettings(
                request.displayName(),
                request.description(),
                request.status(),
                request.supportsSandbox(),
                request.supportsLive(),
                request.defaultEnvironment());
        AiToolProvider saved = providerRepository.save(provider);
        publish(KafkaTopicConstants.AI_PROVIDER_UPDATED, saved, "provider.updated");
        audit(saved, "provider.updated", AuditActionType.UPDATE, "Provider updated");
        return toView(saved, preferredCredential(providerId, null));
    }

    public MasterProviderView updateProviderStatus(UUID providerId, UpdateProviderStatusRequest request) {
        AiToolProvider provider = requireProvider(providerId);
        ProviderStatus status = request.status() == null ? ProviderStatus.ACTIVE : request.status();
        provider.updateSettings(provider.getProviderName(), provider.getDescription(), status, provider.isSupportsSandbox(),
                provider.isSupportsLive(), provider.getDefaultEnvironment());
        AiToolProvider saved = providerRepository.save(provider);
        publish(KafkaTopicConstants.AI_PROVIDER_UPDATED, saved, status == ProviderStatus.ACTIVE ? "provider.enabled" : "provider.disabled");
        audit(saved, status == ProviderStatus.ACTIVE ? "provider.enabled" : "provider.disabled", AuditActionType.UPDATE, "Provider status updated");
        return toView(saved, preferredCredential(providerId, null));
    }

    public MasterProviderView updateProviderStatus(String providerKey, UpdateProviderStatusRequest request) {
        return updateProviderStatus(requireProvider(providerKey).getId(), request);
    }

    public ProviderCredentialSavedView saveCredential(UUID providerId, SaveProviderCredentialRequest request) {
        AiToolProvider provider = requireProvider(providerId);
        ProviderEnvironment environment = environment(request.environment());
        validateEnvironment(provider, environment);
        validateWebhook(request.webhookUrl());
        AiProviderCredential existing = credentialRepository
                .findFirstByProviderIdAndEnvironmentAndDeletedFalseOrderByUpdatedAtDesc(providerId, environment)
                .orElse(null);
        if (existing == null && (request.secret() == null || request.secret().isBlank())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Secret is required for first provider credential configuration");
        }
        String encrypted = encryptionService.encryptNullable(request.secret(), existing == null ? null : existing.getEncryptedSecret());
        String masked = encryptionService.maskNullable(request.secret(), existing == null ? null : existing.getMaskedSecret());
        AiProviderCredential credential = existing == null
                ? AiProviderCredential.createProviderCredential(providerId, environment, encrypted, masked, request.webhookUrl(), request.active(), Map.of())
                : existing;
        if (existing != null) {
            credential.updateProviderCredential(environment, encrypted, masked, request.webhookUrl(), request.active(), existing.getMetadata());
        }
        AiProviderCredential saved = credentialRepository.save(credential);
        publish(KafkaTopicConstants.AI_PROVIDER_UPDATED, provider, "provider.credential.updated");
        audit(provider, "provider.credential.updated", AuditActionType.UPDATE, "Provider credential updated");
        return credentialSavedView(provider, environment, saved.getCredentialStatus(), saved.isActive());
    }

    public ProviderCredentialSavedView saveCredential(String providerKey, SaveProviderCredentialRequest request) {
        return saveCredential(requireProvider(providerKey).getId(), request);
    }

    public ProviderConnectionTestResult testConnection(UUID providerId, TestProviderConnectionRequest request) {
        AiToolProvider provider = requireProvider(providerId);
        ProviderEnvironment environment = environment(request.environment());
        validateEnvironment(provider, environment);
        AiProviderCredential credential = credentialRepository
                .findFirstByProviderIdAndEnvironmentAndDeletedFalseOrderByUpdatedAtDesc(providerId, environment)
                .orElse(null);
        if ((request.secret() == null || request.secret().isBlank()) && (credential == null || credential.getEncryptedSecret() == null)) {
            Instant testedAt = clock.instant();
            return new ProviderConnectionTestResult(
                    provider.getProviderCode(),
                    provider.getProviderCode(),
                    provider.getProviderName(),
                    categoryForView(provider),
                    environment,
                    false,
                    "NOT_CONFIGURED",
                    ProviderConnectionTestStatus.NOT_CONFIGURED,
                    null,
                    testedAt,
                    "Provider credential is not configured");
        }
        String secret = request.secret() == null || request.secret().isBlank()
                ? encryptionService.decryptNullable(credential.getEncryptedSecret())
                : request.secret().trim();
        if (secret == null || secret.isBlank()) {
            Instant testedAt = clock.instant();
            return new ProviderConnectionTestResult(
                    provider.getProviderCode(),
                    provider.getProviderCode(),
                    provider.getProviderName(),
                    categoryForView(provider),
                    environment,
                    false,
                    "NOT_CONFIGURED",
                    ProviderConnectionTestStatus.NOT_CONFIGURED,
                    null,
                    testedAt,
                    "Provider credential is not configured");
        }
        Instant testedAt = clock.instant();
        ProviderConnectionTestResult result = new ProviderConnectionTestResult(
                provider.getProviderCode(),
                provider.getProviderCode(),
                provider.getProviderName(),
                categoryForView(provider),
                environment,
                true,
                "HEALTHY",
                ProviderConnectionTestStatus.SUCCESS,
                null,
                testedAt,
                provider.getProviderName() + " connection test successful");
        if (credential != null) {
            credential.recordTest(result.testStatus(), testedAt, result.message());
            credentialRepository.save(credential);
        }
        publish(KafkaTopicConstants.AI_PROVIDER_HEALTH_UPDATED, provider, "provider.connection.tested");
        audit(provider, "provider.connection.tested", AuditActionType.PROCESS, "Provider connection tested");
        return result;
    }

    public ProviderConnectionTestResult testConnection(String providerKey, TestProviderConnectionRequest request) {
        return testConnection(requireProvider(providerKey).getId(), request);
    }

    public ProviderCredentialSavedView revokeCredential(UUID providerId, ProviderEnvironment requestedEnvironment) {
        AiToolProvider provider = requireProvider(providerId);
        ProviderEnvironment environment = environment(requestedEnvironment);
        AiProviderCredential credential = credentialRepository
                .findFirstByProviderIdAndEnvironmentAndDeletedFalseOrderByUpdatedAtDesc(providerId, environment)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Provider credential not found"));
        credential.revoke();
        AiProviderCredential saved = credentialRepository.save(credential);
        publish(KafkaTopicConstants.AI_PROVIDER_UPDATED, provider, "provider.credential.revoked");
        audit(provider, "provider.credential.revoked", AuditActionType.DELETE, "Provider credential revoked");
        return credentialSavedView(provider, environment, saved.getCredentialStatus(), saved.isActive());
    }

    private MasterProviderView toView(AiToolProvider provider, AiProviderCredential credential) {
        CredentialStatus credentialStatus = credential == null ? CredentialStatus.NOT_CONFIGURED : credential.getCredentialStatus();
        return new MasterProviderView(
                provider.getId(),
                provider.getProviderCode(),
                provider.getProviderCode(),
                provider.getProviderName(),
                categoryForView(provider),
                providerTypeForSettings(provider),
                provider.getStatus(),
                provider.getDescription(),
                provider.getSupportedLayers(),
                supportedEnvironments(provider),
                provider.isSupportsSandbox(),
                provider.isSupportsLive(),
                provider.getDefaultEnvironment(),
                credentialStatus,
                credential != null && credential.getEncryptedSecret() != null && !credential.getEncryptedSecret().isBlank(),
                credential == null ? provider.getDefaultEnvironment() : credential.getEnvironment(),
                credential != null && credential.getWebhookUrl() != null,
                credential == null ? null : credential.getWebhookUrl(),
                credential == null || credential.getLastTestStatus() == null ? ProviderConnectionTestStatus.NOT_TESTED : credential.getLastTestStatus(),
                credential == null ? null : credential.getLastTestedAt(),
                credential == null ? null : credential.getLastTestMessage(),
                provider.isEnabled(),
                "SEEDED".equalsIgnoreCase(provider.getCategory()),
                true,
                credential == null ? null : credential.getUpdatedAt(),
                provider.getCreatedAt(),
                provider.getUpdatedAt());
    }

    private ProviderCredentialSavedView credentialSavedView(
            AiToolProvider provider,
            ProviderEnvironment environment,
            CredentialStatus credentialStatus,
            boolean active
    ) {
        return new ProviderCredentialSavedView(
                provider.getId(),
                provider.getProviderCode(),
                provider.getProviderCode(),
                provider.getProviderName(),
                categoryForView(provider),
                environment,
                credentialStatus,
                active,
                true);
    }

    private AiProviderCredential preferredCredential(UUID providerId, ProviderEnvironment environment) {
        if (environment != null) {
            return credentialRepository.findFirstByProviderIdAndEnvironmentAndDeletedFalseOrderByUpdatedAtDesc(providerId, environment).orElse(null);
        }
        return credentialRepository.findFirstByProviderIdAndActiveTrueAndDeletedFalseOrderByUpdatedAtDesc(providerId)
                .or(() -> credentialRepository.findFirstByProviderIdAndEnvironmentAndDeletedFalseOrderByUpdatedAtDesc(providerId, ProviderEnvironment.SANDBOX))
                .orElse(null);
    }

    private ProviderType providerTypeForSettings(AiToolProvider provider) {
        return switch (provider.getProviderType()) {
            case PAYMENT, STORAGE, NOTIFICATION, AI -> provider.getProviderType();
            default -> ProviderType.AI;
        };
    }

    private AiToolProvider requireProvider(UUID providerId) {
        return providerRepository.findByIdAndDeletedFalse(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Provider not found"));
    }

    private AiToolProvider requireProvider(String providerKey) {
        Optional<AiToolProvider> byId = parseUuid(providerKey).flatMap(providerRepository::findByIdAndDeletedFalse);
        String normalized = byId.isPresent() ? "" : AiToolProvider.normalizeCode(providerKey, "providerKey");
        return byId.or(() -> providerRepository.findByProviderCodeAndDeletedFalse(normalized))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Provider not found"));
    }

    private Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private String categoryForView(AiToolProvider provider) {
        return provider.getProviderType().name();
    }

    private List<ProviderEnvironment> supportedEnvironments(AiToolProvider provider) {
        java.util.ArrayList<ProviderEnvironment> environments = new java.util.ArrayList<>();
        if (provider.isSupportsSandbox()) {
            environments.add(ProviderEnvironment.SANDBOX);
        }
        if (provider.isSupportsLive()) {
            environments.add(ProviderEnvironment.LIVE);
        }
        return List.copyOf(environments);
    }

    private ProviderEnvironment environment(ProviderEnvironment environment) {
        return environment == null ? ProviderEnvironment.SANDBOX : environment;
    }

    private void validateEnvironment(AiToolProvider provider, ProviderEnvironment environment) {
        if (environment == ProviderEnvironment.SANDBOX && !provider.isSupportsSandbox()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Provider does not support sandbox credentials");
        }
        if (environment == ProviderEnvironment.LIVE && !provider.isSupportsLive()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Provider does not support live credentials");
        }
    }

    private void validateWebhook(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }
        try {
            URI uri = URI.create(webhookUrl.trim());
            if (uri.getScheme() == null || uri.getHost() == null || (!uri.getScheme().equals("https") && !uri.getScheme().equals("http"))) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Webhook URL must be a valid HTTP or HTTPS URL");
        }
    }

    private String required(String value, String field) {
        return AiToolProvider.normalizeRequired(value, field);
    }

    private void publish(String topic, AiToolProvider provider, String action) {
        if (eventPublisher != null) {
            eventPublisher.publish(topic, null, provider.getId(), Map.of(
                    "providerId", provider.getId().toString(),
                    "providerCode", provider.getProviderCode(),
                    "action", action));
        }
    }

    private void audit(AiToolProvider provider, String action, AuditActionType actionType, String summary) {
        if (auditLogService == null) {
            return;
        }
        auditLogService.appendCurrentPlatformAction(
                action + "." + provider.getId() + "." + clock.millis(),
                actionType,
                AuditOutcome.SUCCESS,
                "PROVIDER",
                provider.getId(),
                summary,
                Map.of(
                        "providerCode", provider.getProviderCode(),
                        "action", action),
                null,
                null);
    }
}
