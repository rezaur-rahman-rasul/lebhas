package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.AiProviderCredentialCommand;
import com.lebhas.ai.application.dto.AiProviderCredentialView;
import com.lebhas.ai.application.dto.CreativeToolCommand;
import com.lebhas.ai.application.dto.CreativeToolView;
import com.lebhas.ai.application.dto.ProviderHealthSnapshotView;
import com.lebhas.ai.application.dto.ProviderRoutingPolicyCommand;
import com.lebhas.ai.application.dto.ProviderRoutingPolicyView;
import com.lebhas.ai.application.dto.ResolvedProviderRouteView;
import com.lebhas.ai.domain.AiProviderCredential;
import com.lebhas.ai.domain.AiToolProvider;
import com.lebhas.ai.domain.CredentialStatus;
import com.lebhas.ai.domain.CreativeTool;
import com.lebhas.ai.domain.CreativeToolCapability;
import com.lebhas.ai.domain.ProviderHealthSnapshot;
import com.lebhas.ai.domain.ProviderRoutingPolicy;
import com.lebhas.ai.domain.ProviderStatus;
import com.lebhas.ai.domain.ToolCreditCostPolicy;
import com.lebhas.ai.infrastructure.persistence.AiModelRepository;
import com.lebhas.ai.infrastructure.persistence.AiProviderCredentialRepository;
import com.lebhas.ai.infrastructure.persistence.AiToolProviderRepository;
import com.lebhas.ai.infrastructure.persistence.CreativeToolCapabilityRepository;
import com.lebhas.ai.infrastructure.persistence.CreativeToolRepository;
import com.lebhas.ai.infrastructure.persistence.ProviderHealthSnapshotRepository;
import com.lebhas.ai.infrastructure.persistence.ProviderRoutingPolicyRepository;
import com.lebhas.ai.infrastructure.persistence.ToolCreditCostPolicyRepository;
import com.lebhas.ai.provider.AiProviderType;
import com.lebhas.creativesaas.asset.application.AssetEventPublisher;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Transactional
public class MasterAiProviderToolRegistryService {

    private static final String DEFAULT_QUALITY_MODE = "DEFAULT";

    private final AiToolProviderRepository providerRepository;
    private final AiModelRepository modelRepository;
    private final AiProviderCredentialRepository credentialRepository;
    private final CreativeToolRepository toolRepository;
    private final CreativeToolCapabilityRepository toolCapabilityRepository;
    private final ToolCreditCostPolicyRepository costPolicyRepository;
    private final ProviderRoutingPolicyRepository routingPolicyRepository;
    private final ProviderHealthSnapshotRepository healthSnapshotRepository;
    private final AiCredentialEncryptionService encryptionService;
    private final AssetEventPublisher eventPublisher;
    private final Clock clock;

    public MasterAiProviderToolRegistryService(
            AiToolProviderRepository providerRepository,
            AiModelRepository modelRepository,
            AiProviderCredentialRepository credentialRepository,
            CreativeToolRepository toolRepository,
            CreativeToolCapabilityRepository toolCapabilityRepository,
            ToolCreditCostPolicyRepository costPolicyRepository,
            ProviderRoutingPolicyRepository routingPolicyRepository,
            ProviderHealthSnapshotRepository healthSnapshotRepository,
            AiCredentialEncryptionService encryptionService,
            AssetEventPublisher eventPublisher,
            Clock clock
    ) {
        this.providerRepository = providerRepository;
        this.modelRepository = modelRepository;
        this.credentialRepository = credentialRepository;
        this.toolRepository = toolRepository;
        this.toolCapabilityRepository = toolCapabilityRepository;
        this.costPolicyRepository = costPolicyRepository;
        this.routingPolicyRepository = routingPolicyRepository;
        this.healthSnapshotRepository = healthSnapshotRepository;
        this.encryptionService = encryptionService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public AiProviderCredentialView createCredential(UUID providerId, AiProviderCredentialCommand command) {
        requireProvider(providerId);
        String encrypted = encryptionService.encryptNullable(command.secretValue(), null);
        AiProviderCredential credential = AiProviderCredential.create(
                providerId,
                command.credentialName(),
                encrypted,
                encryptionService.maskNullable(command.secretValue(), null),
                command.active(),
                safeMetadata(command.metadata()));
        AiProviderCredential saved = credentialRepository.save(credential);
        return toView(saved);
    }

    public AiProviderCredentialView updateCredential(UUID providerId, UUID credentialId, AiProviderCredentialCommand command) {
        requireProvider(providerId);
        AiProviderCredential credential = credentialRepository.findByIdAndProviderIdAndDeletedFalse(credentialId, providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "AI provider credential not found"));
        credential.update(
                command.credentialName(),
                encryptionService.encryptNullable(command.secretValue(), credential.getEncryptedSecret()),
                encryptionService.maskNullable(command.secretValue(), credential.getMaskedSecret()),
                command.active(),
                safeMetadata(command.metadata()));
        return toView(credentialRepository.save(credential));
    }

    public CreativeToolView createTool(CreativeToolCommand command) {
        if (toolRepository.existsByToolCodeAndDeletedFalse(AiToolProvider.normalizeCode(command.toolCode(), "toolCode"))) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Creative tool code already exists");
        }
        CreativeTool tool = CreativeTool.create(
                command.toolCode(),
                command.toolName(),
                command.toolCategory(),
                command.enabled(),
                command.description(),
                safeMetadata(command.metadata()));
        CreativeTool saved = toolRepository.save(tool);
        saveToolDetails(saved, command);
        publish(KafkaTopicConstants.CREATIVE_TOOL_CREATED, saved.getId(), Map.of("toolId", saved.getId().toString(), "toolCode", saved.getToolCode()));
        return toolView(saved);
    }

    public CreativeToolView updateTool(UUID toolId, CreativeToolCommand command) {
        CreativeTool tool = requireTool(toolId);
        tool.update(command.toolName(), command.toolCategory(), command.enabled(), command.description(), safeMetadata(command.metadata()));
        CreativeTool saved = toolRepository.save(tool);
        saveToolDetails(saved, command);
        return toolView(saved);
    }

    @Transactional(readOnly = true)
    public List<CreativeToolView> listTools() {
        return toolRepository.findAllByDeletedFalseOrderByToolNameAsc().stream().map(this::toolView).toList();
    }

    @Transactional(readOnly = true)
    public CreativeToolView getTool(UUID toolId) {
        return toolView(requireTool(toolId));
    }

    public ProviderRoutingPolicyView createRoutingPolicy(ProviderRoutingPolicyCommand command) {
        requireTool(command.toolId());
        requireUsableProvider(command.providerId());
        requireConfiguredCredentialIfActive(command.providerId(), command.enabled());
        requireModel(command.modelId());
        requireProviderIfPresent(command.fallbackProviderId());
        requireConfiguredCredentialIfActive(command.fallbackProviderId(), command.enabled());
        requireModel(command.fallbackModelId());
        ProviderRoutingPolicy policy = ProviderRoutingPolicy.create(
                command.policyCode(),
                command.toolId(),
                qualityMode(command.qualityMode()),
                command.providerId(),
                command.modelId(),
                command.fallbackProviderId(),
                command.fallbackModelId(),
                command.priorityOrder(),
                command.enabled(),
                command.circuitFailureThreshold(),
                safeMetadata(command.metadata()));
        ProviderRoutingPolicy saved = routingPolicyRepository.save(policy);
        publish(KafkaTopicConstants.PROVIDER_ROUTING_POLICY_UPDATED, saved.getId(), Map.of("policyId", saved.getId().toString()));
        return toView(saved);
    }

    public ProviderRoutingPolicyView updateRoutingPolicy(UUID policyId, ProviderRoutingPolicyCommand command) {
        ProviderRoutingPolicy policy = routingPolicyRepository.findByIdAndDeletedFalse(policyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Provider routing policy not found"));
        requireTool(command.toolId());
        requireUsableProvider(command.providerId());
        requireConfiguredCredentialIfActive(command.providerId(), command.enabled());
        requireModel(command.modelId());
        requireProviderIfPresent(command.fallbackProviderId());
        requireConfiguredCredentialIfActive(command.fallbackProviderId(), command.enabled());
        requireModel(command.fallbackModelId());
        policy.update(
                command.toolId(),
                qualityMode(command.qualityMode()),
                command.providerId(),
                command.modelId(),
                command.fallbackProviderId(),
                command.fallbackModelId(),
                command.priorityOrder(),
                command.enabled(),
                command.circuitFailureThreshold(),
                safeMetadata(command.metadata()));
        ProviderRoutingPolicy saved = routingPolicyRepository.save(policy);
        publish(KafkaTopicConstants.PROVIDER_ROUTING_POLICY_UPDATED, saved.getId(), Map.of("policyId", saved.getId().toString()));
        return toView(saved);
    }

    @Transactional(readOnly = true)
    public List<ProviderRoutingPolicyView> listRoutingPolicies() {
        return routingPolicyRepository.findAllByDeletedFalseOrderByPriorityOrderAscPolicyCodeAsc().stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public ProviderRoutingPolicyView getRoutingPolicy(UUID policyId) {
        return routingPolicyRepository.findByIdAndDeletedFalse(policyId)
                .map(this::toView)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Provider routing policy not found"));
    }

    @Transactional(readOnly = true)
    public ResolvedProviderRouteView resolveProvider(UUID toolId, String qualityMode) {
        CreativeTool tool = requireTool(toolId);
        if (!tool.isEnabled()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Creative tool is disabled");
        }
        List<ProviderRoutingPolicy> policies = routingPolicyRepository
                .findAllByToolIdAndQualityModeAndEnabledTrueAndDeletedFalseOrderByPriorityOrderAsc(toolId, qualityMode(qualityMode));
        if (policies.isEmpty()) {
            AiToolProvider mock = providerRepository.findByProviderCodeAndDeletedFalse(AiProviderType.MOCK.name())
                    .filter(provider -> provider.isEnabled() && provider.getStatus() == ProviderStatus.ACTIVE)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "No provider routing policy is configured"));
            return new ResolvedProviderRouteView(null, toolId, qualityMode(qualityMode), mock.getId(), null, false, "mock_default");
        }
        for (ProviderRoutingPolicy policy : policies) {
            AiToolProvider provider = requireProvider(policy.getProviderId());
            if (isSelectable(provider) && !isCircuitOpen(policy.getProviderId(), policy.getCircuitFailureThreshold())) {
                return new ResolvedProviderRouteView(policy.getId(), toolId, policy.getQualityMode(), policy.getProviderId(), policy.getModelId(), false, "primary");
            }
            if (policy.getFallbackProviderId() != null) {
                AiToolProvider fallback = requireProvider(policy.getFallbackProviderId());
                if (isSelectable(fallback) && !isCircuitOpen(fallback.getId(), policy.getCircuitFailureThreshold())) {
                    return new ResolvedProviderRouteView(policy.getId(), toolId, policy.getQualityMode(), fallback.getId(), policy.getFallbackModelId(), true, "fallback");
                }
            }
        }
        throw new BusinessException(
                ErrorCode.GENERATION_PROVIDER_UNAVAILABLE,
                "No active AI provider with configured credentials is available for this creative tool");
    }

    public ProviderHealthSnapshotView recordHealth(UUID providerId, String status, int consecutiveFailures, String failureReason, Map<String, Object> metadata) {
        requireProvider(providerId);
        ProviderHealthSnapshot snapshot = ProviderHealthSnapshot.create(
                providerId,
                status,
                consecutiveFailures,
                consecutiveFailures >= 3,
                clock.instant(),
                failureReason,
                safeMetadata(metadata));
        ProviderHealthSnapshot saved = healthSnapshotRepository.save(snapshot);
        publish(KafkaTopicConstants.AI_PROVIDER_HEALTH_UPDATED, saved.getProviderId(), Map.of(
                "providerId", saved.getProviderId().toString(),
                "status", saved.getStatus(),
                "circuitOpen", saved.isCircuitOpen()));
        return toView(saved);
    }

    @Transactional(readOnly = true)
    public List<ProviderHealthSnapshotView> listHealth() {
        return healthSnapshotRepository.findAllByDeletedFalseOrderByLastCheckedAtDesc().stream().map(this::toView).toList();
    }

    private void saveToolDetails(CreativeTool tool, CreativeToolCommand command) {
        if (command.capabilities() != null) {
            command.capabilities().forEach(capability -> toolCapabilityRepository.save(CreativeToolCapability.create(
                    tool.getId(),
                    capability.capabilityCode(),
                    capability.enabled(),
                    safeMetadata(capability.metadata()))));
        }
        if (command.costPolicy() != null) {
            costPolicyRepository.save(ToolCreditCostPolicy.create(
                    tool.getId(),
                    command.costPolicy().policyCode(),
                    command.costPolicy().creditCost(),
                    command.costPolicy().enabled(),
                    command.costPolicy().effectiveFrom(),
                    command.costPolicy().effectiveUntil(),
                    safeMetadata(command.costPolicy().metadata())));
        }
    }

    private boolean isCircuitOpen(UUID providerId, int threshold) {
        return healthSnapshotRepository.findFirstByProviderIdAndDeletedFalseOrderByLastCheckedAtDesc(providerId)
                .map(snapshot -> snapshot.isCircuitOpen() || snapshot.getConsecutiveFailures() >= threshold)
                .orElse(false);
    }

    private boolean isSelectable(AiToolProvider provider) {
        if (!isActiveProvider(provider)) {
            return false;
        }
        if (AiProviderType.MOCK.name().equals(provider.getProviderCode())) {
            return true;
        }
        return hasConfiguredCredential(provider.getId());
    }

    private boolean isActiveProvider(AiToolProvider provider) {
        return provider.isEnabled() && provider.getStatus() == ProviderStatus.ACTIVE;
    }

    private boolean hasConfiguredCredential(UUID providerId) {
        return credentialRepository
                .findFirstByProviderIdAndActiveTrueAndDeletedFalseOrderByUpdatedAtDesc(providerId)
                .filter(credential -> credential.getCredentialStatus() == CredentialStatus.CONFIGURED)
                .filter(credential -> credential.getEncryptedSecret() != null && !credential.getEncryptedSecret().isBlank())
                .isPresent();
    }

    private AiToolProvider requireUsableProvider(UUID providerId) {
        AiToolProvider provider = requireProvider(providerId);
        if (!isActiveProvider(provider)) {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "Disabled AI provider cannot be used");
        }
        return provider;
    }

    private void requireProviderIfPresent(UUID providerId) {
        if (providerId != null) {
            requireProvider(providerId);
        }
    }

    private void requireConfiguredCredentialIfActive(UUID providerId, boolean enabled) {
        if (!enabled || providerId == null) {
            return;
        }
        if (!hasConfiguredCredential(providerId)) {
            throw new BusinessException(
                    ErrorCode.AI_ROUTING_POLICY_INVALID,
                    "Active routing policy requires configured provider credentials");
        }
    }

    private AiToolProvider requireProvider(UUID providerId) {
        return providerRepository.findByIdAndDeletedFalse(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "AI provider not found"));
    }

    private CreativeTool requireTool(UUID toolId) {
        return toolRepository.findByIdAndDeletedFalse(toolId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Creative tool not found"));
    }

    private void requireModel(UUID modelId) {
        if (modelId != null) {
            modelRepository.findById(modelId)
                    .filter(model -> !model.isDeleted())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "AI model not found"));
        }
    }

    private CreativeToolView toolView(CreativeTool tool) {
        return new CreativeToolView(
                tool.getId(),
                tool.getToolCode(),
                tool.getToolName(),
                tool.getToolCategory(),
                tool.isEnabled(),
                tool.getDescription(),
                toolCapabilityRepository.findAllByToolIdAndDeletedFalseOrderByCapabilityCodeAsc(tool.getId()).stream()
                        .map(capability -> new CreativeToolView.CreativeToolCapabilityView(
                                capability.getId(),
                                capability.getCapabilityCode(),
                                capability.isEnabled(),
                                capability.getMetadata()))
                        .toList(),
                costPolicyRepository.findAllByToolIdAndDeletedFalseOrderByPolicyCodeAsc(tool.getId()).stream()
                        .map(policy -> new CreativeToolView.ToolCreditCostPolicyView(
                                policy.getId(),
                                policy.getPolicyCode(),
                                policy.getCreditCost(),
                                policy.isEnabled(),
                                policy.getEffectiveFrom(),
                                policy.getEffectiveUntil(),
                                policy.getMetadata()))
                        .toList(),
                tool.getMetadata());
    }

    private AiProviderCredentialView toView(AiProviderCredential credential) {
        return new AiProviderCredentialView(
                credential.getId(),
                credential.getProviderId(),
                credential.getCredentialName(),
                credential.getMaskedSecret(),
                credential.isActive(),
                credential.getMetadata());
    }

    private ProviderRoutingPolicyView toView(ProviderRoutingPolicy policy) {
        return new ProviderRoutingPolicyView(
                policy.getId(),
                policy.getPolicyCode(),
                policy.getToolId(),
                policy.getQualityMode(),
                policy.getProviderId(),
                policy.getModelId(),
                policy.getFallbackProviderId(),
                policy.getFallbackModelId(),
                policy.getPriorityOrder(),
                policy.isEnabled(),
                policy.getCircuitFailureThreshold(),
                policy.getMetadata());
    }

    private ProviderHealthSnapshotView toView(ProviderHealthSnapshot snapshot) {
        return new ProviderHealthSnapshotView(
                snapshot.getId(),
                snapshot.getProviderId(),
                snapshot.getStatus(),
                snapshot.getConsecutiveFailures(),
                snapshot.isCircuitOpen(),
                snapshot.getLastCheckedAt(),
                snapshot.getFailureReason(),
                snapshot.getMetadata());
    }

    private String qualityMode(String qualityMode) {
        return qualityMode == null || qualityMode.isBlank()
                ? DEFAULT_QUALITY_MODE
                : AiToolProvider.normalizeCode(qualityMode, "qualityMode");
    }

    private Map<String, Object> safeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> sanitized = new java.util.LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            String normalized = key == null ? "" : key.toLowerCase();
            if (!normalized.contains("secret") && !normalized.contains("key")) {
                sanitized.put(key, value);
            }
        });
        return sanitized;
    }

    private void publish(String topic, UUID aggregateId, Map<String, Object> attributes) {
        if (eventPublisher != null) {
            eventPublisher.publish(topic, null, aggregateId, attributes);
        }
    }
}
