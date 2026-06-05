package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.CreateMasterProviderRequest;
import com.lebhas.ai.application.dto.MasterProviderView;
import com.lebhas.ai.application.dto.ProviderConnectionTestResult;
import com.lebhas.ai.application.dto.ProviderCredentialSavedView;
import com.lebhas.ai.application.dto.ProviderModelsJsonView;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
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
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String AVAILABLE_CREDIT_BALANCE_METADATA_KEY = "availableCreditBalance";
    private static final String BASE_URL_METADATA_KEY = "baseUrl";
    private static final String DEFAULT_MODEL_METADATA_KEY = "defaultModel";
    private static final String METADATA_JSON_KEY = "metadataJson";
    private static final String COST_MULTIPLIER_METADATA_KEY = "costMultiplier";
    private static final String PRIORITY_METADATA_KEY = "priority";
    private static final String RATE_LIMIT_PER_MINUTE_METADATA_KEY = "rateLimitPerMinute";
    private static final String PROVIDER_JSON_ENDPOINT_METADATA_KEY = "jsonEndpoint";
    private static final String PROVIDER_MODELS_ENDPOINT_METADATA_KEY = "modelsEndpoint";
    private static final String PROVIDER_HEALTH_ENDPOINT_METADATA_KEY = "healthEndpoint";
    private static final String PROVIDER_ENDPOINT_PATH_METADATA_KEY = "endpointPath";
    private static final String PROVIDER_ENDPOINT_AUTH_METADATA_KEY = "endpointAuth";
    private static final String PROVIDER_API_KEY_QUERY_PARAM_METADATA_KEY = "apiKeyQueryParam";

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
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
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
                supportedCapabilities(request.supportedCapabilities()),
                providerCode + "_API_KEY",
                true,
                true,
                true,
                costMetadata(
                        request.baseUrl(),
                        request.defaultModel(),
                        request.modelsEndpoint(),
                        request.modelsEndpointAuth(),
                        request.apiKeyQueryParam(),
                        request.metadataJson(),
                        request.costMultiplier()),
                Map.of(),
                rateLimitMetadata(request.priority(), request.rateLimitPerMinute()));
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
        ProviderStatus status = request.status() == null ? provider.getStatus() : request.status();
        provider.updateSettings(
                request.displayName(),
                request.description(),
                status,
                request.supportsSandbox(),
                request.supportsLive(),
                request.defaultEnvironment());
        provider.update(
                request.displayName(),
                provider.getProviderType(),
                status,
                status == ProviderStatus.ACTIVE,
                supportedCapabilities(request.supportedCapabilities()),
                provider.getCredentialConfigKey(),
                provider.isFallbackEligible(),
                provider.isWorkspaceRoutingEligible(),
                provider.isPlanRoutingEligible(),
                costMetadata(
                        request.baseUrl(),
                        request.defaultModel(),
                        request.modelsEndpoint(),
                        request.modelsEndpointAuth(),
                        request.apiKeyQueryParam(),
                        request.metadataJson(),
                        request.costMultiplier()),
                provider.getQualityMetadata(),
                rateLimitMetadata(request.priority(), request.rateLimitPerMinute()));
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
        Map<String, Object> metadata = credentialMetadata(
                existing == null ? Map.of() : existing.getMetadata(),
                request.availableCreditBalance());
        AiProviderCredential credential = existing == null
                ? AiProviderCredential.createProviderCredential(providerId, environment, encrypted, masked, request.webhookUrl(), request.active(), metadata)
                : existing;
        if (existing != null) {
            credential.updateProviderCredential(environment, encrypted, masked, request.webhookUrl(), request.active(), metadata);
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
        ProviderConnectionTestResult result = runRealConnectionTest(provider, environment, secret);
        if (credential != null) {
            credential.recordTest(result.testStatus(), result.testedAt(), result.message());
            credentialRepository.save(credential);
        }
        publish(KafkaTopicConstants.AI_PROVIDER_HEALTH_UPDATED, provider, "provider.connection.tested");
        audit(provider, "provider.connection.tested", AuditActionType.PROCESS, "Provider connection tested");
        return result;
    }

    public ProviderConnectionTestResult testConnection(String providerKey, TestProviderConnectionRequest request) {
        return testConnection(requireProvider(providerKey).getId(), request);
    }

    @Transactional(readOnly = true)
    public ProviderModelsJsonView fetchModelsJson(String providerKey, TestProviderConnectionRequest request) {
        AiToolProvider provider = requireProvider(providerKey);
        ProviderEnvironment environment = environment(request.environment());
        validateEnvironment(provider, environment);
        String secret = resolveConnectionSecret(provider.getId(), environment, request);
        Instant startedAt = clock.instant();
        try {
            ProviderJsonEndpointRequest endpointRequest = providerJsonEndpointRequest(provider, secret);
            HttpResponse<String> response = httpClient.send(endpointRequest.request(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(
                        ErrorCode.BUSINESS_RULE_VIOLATION,
                        response.statusCode() == 401 || response.statusCode() == 403
                                ? provider.getProviderName() + " rejected the configured credential"
                                : provider.getProviderName() + " JSON endpoint failed with HTTP " + response.statusCode());
            }
            Map<String, Object> modelsJson = objectMapper.readValue(response.body(), new TypeReference<>() {
            });
            audit(provider, "provider.models-json.fetched", AuditActionType.PROCESS, "Provider models JSON fetched");
            return new ProviderModelsJsonView(
                    provider.getProviderCode(),
                    provider.getProviderCode(),
                    provider.getProviderName(),
                    environment,
                    endpointRequest.safeEndpoint(),
                    response.statusCode(),
                    startedAt,
                    modelsJson);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, provider.getProviderName() + " JSON endpoint could not be reached or did not return valid JSON");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, provider.getProviderName() + " JSON endpoint request was interrupted");
        }
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

    public void deleteProvider(UUID providerId) {
        AiToolProvider provider = requireProvider(providerId);
        provider.disable();
        provider.markDeleted();
        credentialRepository.findAllByProviderIdAndDeletedFalseOrderByCredentialNameAsc(providerId)
                .forEach(credential -> {
                    credential.revoke();
                    credential.markDeleted();
                    credentialRepository.save(credential);
                });
        providerRepository.save(provider);
        publish(KafkaTopicConstants.AI_PROVIDER_UPDATED, provider, "provider.deleted");
        audit(provider, "provider.deleted", AuditActionType.DELETE, "Provider deleted");
    }

    public void deleteProvider(String providerKey) {
        deleteProvider(requireProvider(providerKey).getId());
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
                metadataString(provider.getCostMetadata(), BASE_URL_METADATA_KEY),
                metadataString(provider.getCostMetadata(), DEFAULT_MODEL_METADATA_KEY),
                metadataString(provider.getCostMetadata(), PROVIDER_MODELS_ENDPOINT_METADATA_KEY),
                metadataString(provider.getCostMetadata(), PROVIDER_ENDPOINT_AUTH_METADATA_KEY),
                metadataString(provider.getCostMetadata(), PROVIDER_API_KEY_QUERY_PARAM_METADATA_KEY),
                supportedEnvironments(provider),
                provider.isSupportsSandbox(),
                provider.isSupportsLive(),
                provider.getDefaultEnvironment(),
                credentialStatus,
                credential != null && credential.getEncryptedSecret() != null && !credential.getEncryptedSecret().isBlank(),
                credential == null ? provider.getDefaultEnvironment() : credential.getEnvironment(),
                credential == null ? null : availableCreditBalance(credential.getMetadata()),
                credential != null && credential.getWebhookUrl() != null,
                credential == null ? null : credential.getWebhookUrl(),
                credential == null || credential.getLastTestStatus() == null ? ProviderConnectionTestStatus.NOT_TESTED : credential.getLastTestStatus(),
                credential == null ? null : credential.getLastTestedAt(),
                credential == null ? null : credential.getLastTestMessage(),
                provider.isEnabled(),
                "SEEDED".equalsIgnoreCase(provider.getCategory()),
                credential == null ? null : credential.getMaskedSecret(),
                metadataInteger(provider.getRateLimitMetadata(), PRIORITY_METADATA_KEY, 100),
                metadataInteger(provider.getRateLimitMetadata(), RATE_LIMIT_PER_MINUTE_METADATA_KEY, 60),
                metadataDecimal(provider.getCostMetadata(), COST_MULTIPLIER_METADATA_KEY, BigDecimal.ONE),
                metadataString(provider.getCostMetadata(), METADATA_JSON_KEY),
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

    private Map<String, Object> credentialMetadata(Map<String, Object> existingMetadata, BigDecimal availableCreditBalance) {
        Map<String, Object> metadata = new LinkedHashMap<>(existingMetadata == null ? Map.of() : existingMetadata);
        if (availableCreditBalance == null) {
            metadata.remove(AVAILABLE_CREDIT_BALANCE_METADATA_KEY);
            return Map.copyOf(metadata);
        }
        if (availableCreditBalance.signum() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Available credit balance cannot be negative");
        }
        metadata.put(AVAILABLE_CREDIT_BALANCE_METADATA_KEY, availableCreditBalance);
        return Map.copyOf(metadata);
    }

    private List<String> supportedCapabilities(List<String> requestedCapabilities) {
        if (requestedCapabilities == null || requestedCapabilities.isEmpty()) {
            return List.of();
        }
        return requestedCapabilities.stream()
                .map(AiToolProvider::normalizeNullable)
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.toUpperCase(java.util.Locale.ROOT))
                .distinct()
                .toList();
    }

    private Map<String, Object> costMetadata(
            String baseUrl,
            String defaultModel,
            String modelsEndpoint,
            String modelsEndpointAuth,
            String apiKeyQueryParam,
            String metadataJson,
            BigDecimal costMultiplier
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        putIfPresent(metadata, BASE_URL_METADATA_KEY, baseUrl);
        putIfPresent(metadata, DEFAULT_MODEL_METADATA_KEY, defaultModel);
        putIfPresent(metadata, PROVIDER_MODELS_ENDPOINT_METADATA_KEY, modelsEndpoint);
        putIfPresent(metadata, PROVIDER_ENDPOINT_AUTH_METADATA_KEY, modelsEndpointAuth);
        putIfPresent(metadata, PROVIDER_API_KEY_QUERY_PARAM_METADATA_KEY, apiKeyQueryParam);
        putIfPresent(metadata, METADATA_JSON_KEY, metadataJson);
        metadata.put(COST_MULTIPLIER_METADATA_KEY, normalizePositive(costMultiplier, BigDecimal.ONE, "Cost multiplier"));
        return Map.copyOf(metadata);
    }

    private Map<String, Object> rateLimitMetadata(Integer priority, Integer rateLimitPerMinute) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(PRIORITY_METADATA_KEY, normalizeMin(priority, 100, 0, "Priority"));
        metadata.put(RATE_LIMIT_PER_MINUTE_METADATA_KEY, normalizeMin(rateLimitPerMinute, 60, 1, "Rate limit per minute"));
        return Map.copyOf(metadata);
    }

    private void putIfPresent(Map<String, Object> metadata, String key, String value) {
        String normalized = AiToolProvider.normalizeNullable(value);
        if (normalized != null) {
            metadata.put(key, normalized);
        }
    }

    private BigDecimal normalizePositive(BigDecimal value, BigDecimal defaultValue, String field) {
        BigDecimal normalized = value == null ? defaultValue : value;
        if (normalized.signum() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, field + " must be positive");
        }
        return normalized;
    }

    private Integer normalizeMin(Integer value, int defaultValue, int min, String field) {
        int normalized = value == null ? defaultValue : value;
        if (normalized < min) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, field + " must be at least " + min);
        }
        return normalized;
    }

    private String metadataString(Map<String, Object> metadata, String key) {
        Object value = metadata == null ? null : metadata.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Integer metadataInteger(Map<String, Object> metadata, String key, int defaultValue) {
        Object value = metadata == null ? null : metadata.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Integer.parseInt(stringValue.trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private BigDecimal metadataDecimal(Map<String, Object> metadata, String key, BigDecimal defaultValue) {
        Object value = metadata == null ? null : metadata.get(key);
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return new BigDecimal(stringValue.trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private BigDecimal availableCreditBalance(Map<String, Object> metadata) {
        if (metadata == null || !metadata.containsKey(AVAILABLE_CREDIT_BALANCE_METADATA_KEY)) {
            return null;
        }
        Object value = metadata.get(AVAILABLE_CREDIT_BALANCE_METADATA_KEY);
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return new BigDecimal(stringValue.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
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

    private ProviderConnectionTestResult runRealConnectionTest(
            AiToolProvider provider,
            ProviderEnvironment environment,
            String secret
    ) {
        Instant startedAt = clock.instant();
        if (!"OPENAI".equals(provider.getProviderCode())) {
            return new ProviderConnectionTestResult(
                    provider.getProviderCode(),
                    provider.getProviderCode(),
                    provider.getProviderName(),
                    categoryForView(provider),
                    environment,
                    false,
                    "NOT_IMPLEMENTED",
                    ProviderConnectionTestStatus.NOT_IMPLEMENTED,
                    null,
                    startedAt,
                    "Real connection test is not implemented for this provider");
        }

        try {
            HttpResponse<String> response = httpClient.send(openAiModelsRequest(secret), HttpResponse.BodyHandlers.ofString());
            long latencyMs = Math.max(0L, Duration.between(startedAt, clock.instant()).toMillis());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return new ProviderConnectionTestResult(
                        provider.getProviderCode(),
                        provider.getProviderCode(),
                        provider.getProviderName(),
                        categoryForView(provider),
                        environment,
                        true,
                        "HEALTHY",
                        ProviderConnectionTestStatus.SUCCESS,
                        latencyMs,
                        startedAt,
                        "OpenAI credential accepted");
            }
            return new ProviderConnectionTestResult(
                    provider.getProviderCode(),
                    provider.getProviderCode(),
                    provider.getProviderName(),
                    categoryForView(provider),
                    environment,
                    false,
                    response.statusCode() == 401 || response.statusCode() == 403 ? "INVALID_CREDENTIAL" : "FAILED",
                    ProviderConnectionTestStatus.FAILED,
                    latencyMs,
                    startedAt,
                    response.statusCode() == 401 || response.statusCode() == 403
                            ? "OpenAI rejected the configured credential"
                            : "OpenAI connection test failed with HTTP " + response.statusCode());
        } catch (IOException exception) {
            return failedConnectionResult(provider, environment, startedAt, "OpenAI connection test could not reach the provider");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return failedConnectionResult(provider, environment, startedAt, "OpenAI connection test was interrupted");
        }
    }

    private String resolveConnectionSecret(UUID providerId, ProviderEnvironment environment, TestProviderConnectionRequest request) {
        AiProviderCredential credential = credentialRepository
                .findFirstByProviderIdAndEnvironmentAndDeletedFalseOrderByUpdatedAtDesc(providerId, environment)
                .orElse(null);
        if ((request.secret() == null || request.secret().isBlank()) && (credential == null || credential.getEncryptedSecret() == null)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Provider credential is not configured");
        }
        String secret = request.secret() == null || request.secret().isBlank()
                ? encryptionService.decryptNullable(credential.getEncryptedSecret())
                : request.secret().trim();
        if (secret == null || secret.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Provider credential is not configured");
        }
        return secret;
    }

    private ProviderJsonEndpointRequest providerJsonEndpointRequest(AiToolProvider provider, String secret) {
        Map<String, Object> metadata = parsedMetadataJson(provider);
        String configuredEndpoint = metadataString(provider.getCostMetadata(), PROVIDER_MODELS_ENDPOINT_METADATA_KEY);
        if (configuredEndpoint == null) {
            configuredEndpoint = firstMetadataString(metadata, PROVIDER_JSON_ENDPOINT_METADATA_KEY, PROVIDER_MODELS_ENDPOINT_METADATA_KEY, PROVIDER_HEALTH_ENDPOINT_METADATA_KEY);
        }
        if (configuredEndpoint == null) {
            String endpointPath = firstMetadataString(metadata, PROVIDER_ENDPOINT_PATH_METADATA_KEY);
            String baseUrl = metadataString(provider.getCostMetadata(), BASE_URL_METADATA_KEY);
            if (endpointPath != null && baseUrl != null && !baseUrl.isBlank()) {
                configuredEndpoint = joinUrl(baseUrl, endpointPath);
            }
        }
        if (configuredEndpoint != null) {
            validateProviderEndpoint(configuredEndpoint);
            String authMode = metadataString(provider.getCostMetadata(), PROVIDER_ENDPOINT_AUTH_METADATA_KEY);
            if (authMode == null) {
                authMode = firstMetadataString(metadata, PROVIDER_ENDPOINT_AUTH_METADATA_KEY);
            }
            String queryParam = metadataString(provider.getCostMetadata(), PROVIDER_API_KEY_QUERY_PARAM_METADATA_KEY);
            if (queryParam == null) {
                queryParam = firstMetadataString(metadata, PROVIDER_API_KEY_QUERY_PARAM_METADATA_KEY);
            }
            return configuredProviderJsonEndpoint(configuredEndpoint, authMode, queryParam, secret);
        }

        throw new BusinessException(
                ErrorCode.BUSINESS_RULE_VIOLATION,
                "Models endpoint is not configured for " + provider.getProviderCode());
    }

    private ProviderJsonEndpointRequest configuredProviderJsonEndpoint(String endpoint, String authMode, String queryParam, String secret) {
        String normalizedAuthMode = authMode == null ? "BEARER" : authMode.trim().toUpperCase(java.util.Locale.ROOT);
        return switch (normalizedAuthMode) {
            case "NONE" -> noAuthJsonEndpoint(endpoint);
            case "API_KEY_QUERY", "QUERY_KEY" -> queryKeyJsonEndpoint(endpoint, queryParam == null ? "key" : queryParam, secret);
            case "BEARER" -> bearerJsonEndpoint(endpoint, secret);
            default -> throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Unsupported provider endpointAuth: " + authMode);
        };
    }

    private ProviderJsonEndpointRequest bearerJsonEndpoint(String endpoint, String secret) {
        return new ProviderJsonEndpointRequest(
                endpoint,
                HttpRequest.newBuilder(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(15))
                        .header("Authorization", "Bearer " + secret)
                        .GET()
                        .build());
    }

    private ProviderJsonEndpointRequest queryKeyJsonEndpoint(String endpoint, String queryParam, String secret) {
        String safeEndpoint = appendQueryParam(endpoint, queryParam, "***");
        String endpointWithSecret = appendQueryParam(endpoint, queryParam, secret);
        return new ProviderJsonEndpointRequest(
                safeEndpoint,
                HttpRequest.newBuilder(URI.create(endpointWithSecret))
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build());
    }

    private ProviderJsonEndpointRequest noAuthJsonEndpoint(String endpoint) {
        return new ProviderJsonEndpointRequest(
                endpoint,
                HttpRequest.newBuilder(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build());
    }

    private HttpRequest openAiModelsRequest(String secret) {
        return bearerJsonEndpoint("https://api.openai.com/v1/models", secret).request();
    }

    private Map<String, Object> parsedMetadataJson(AiToolProvider provider) {
        String metadataJson = metadataString(provider.getCostMetadata(), METADATA_JSON_KEY);
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<>() {
            });
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Provider metadataJson must be valid JSON");
        }
    }

    private String firstMetadataString(Map<String, Object> metadata, String... keys) {
        for (String key : keys) {
            Object value = metadata.get(key);
            if (value instanceof String stringValue && !stringValue.isBlank()) {
                return stringValue.trim();
            }
        }
        return null;
    }

    private String joinUrl(String baseUrl, String path) {
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBase + normalizedPath;
    }

    private String appendQueryParam(String endpoint, String queryParam, String value) {
        String separator = endpoint.contains("?") ? "&" : "?";
        return endpoint + separator + URLEncoder.encode(queryParam, StandardCharsets.UTF_8)
                + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void validateProviderEndpoint(String endpoint) {
        try {
            URI uri = URI.create(endpoint);
            if (uri.getScheme() == null || uri.getHost() == null || (!uri.getScheme().equals("https") && !uri.getScheme().equals("http"))) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Models endpoint must be a valid HTTP or HTTPS URL");
        }
    }

    private record ProviderJsonEndpointRequest(String safeEndpoint, HttpRequest request) {
    }

    private ProviderConnectionTestResult failedConnectionResult(
            AiToolProvider provider,
            ProviderEnvironment environment,
            Instant startedAt,
            String message
    ) {
        return new ProviderConnectionTestResult(
                provider.getProviderCode(),
                provider.getProviderCode(),
                provider.getProviderName(),
                categoryForView(provider),
                environment,
                false,
                "FAILED",
                ProviderConnectionTestStatus.FAILED,
                Math.max(0L, Duration.between(startedAt, clock.instant()).toMillis()),
                startedAt,
                message);
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
