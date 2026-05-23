package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.AiModelCommand;
import com.lebhas.ai.application.dto.AiProviderCommand;
import com.lebhas.ai.application.dto.AiProviderView;
import com.lebhas.ai.application.dto.AiToolCapabilityCommand;
import com.lebhas.ai.domain.AiModel;
import com.lebhas.ai.domain.AiToolCapability;
import com.lebhas.ai.domain.AiToolProvider;
import com.lebhas.ai.domain.ProviderStatus;
import com.lebhas.ai.infrastructure.persistence.AiModelRepository;
import com.lebhas.ai.infrastructure.persistence.AiToolCapabilityRepository;
import com.lebhas.ai.infrastructure.persistence.AiToolProviderRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Transactional
public class MasterAiProviderManagementService {

    private final AiToolProviderRepository providerRepository;
    private final AiModelRepository modelRepository;
    private final AiToolCapabilityRepository capabilityRepository;
    private final AiProviderRegistryMapper mapper;

    public MasterAiProviderManagementService(
            AiToolProviderRepository providerRepository,
            AiModelRepository modelRepository,
            AiToolCapabilityRepository capabilityRepository,
            AiProviderRegistryMapper mapper
    ) {
        this.providerRepository = providerRepository;
        this.modelRepository = modelRepository;
        this.capabilityRepository = capabilityRepository;
        this.mapper = mapper;
    }

    public AiProviderView createProvider(AiProviderCommand command) {
        if (providerRepository.existsByProviderCodeAndDeletedFalse(AiToolProvider.normalizeCode(command.providerCode(), "providerCode"))) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "AI provider code already exists");
        }
        AiToolProvider provider = AiToolProvider.create(
                command.providerCode(),
                command.providerName(),
                command.providerType(),
                command.status(),
                command.enabled(),
                command.supportedLayers(),
                command.credentialConfigKey(),
                command.fallbackEligible(),
                command.workspaceRoutingEligible(),
                command.planRoutingEligible(),
                command.costMetadata(),
                command.qualityMetadata(),
                command.rateLimitMetadata());
        return providerView(providerRepository.save(provider));
    }

    public AiProviderView updateProvider(UUID providerId, AiProviderCommand command) {
        AiToolProvider provider = provider(providerId);
        provider.update(
                command.providerName(),
                command.providerType(),
                command.status(),
                command.enabled(),
                command.supportedLayers(),
                command.credentialConfigKey(),
                command.fallbackEligible(),
                command.workspaceRoutingEligible(),
                command.planRoutingEligible(),
                command.costMetadata(),
                command.qualityMetadata(),
                command.rateLimitMetadata());
        return providerView(providerRepository.save(provider));
    }

    public AiProviderView enableProvider(UUID providerId) {
        AiToolProvider provider = provider(providerId);
        provider.enable();
        return providerView(providerRepository.save(provider));
    }

    public AiProviderView disableProvider(UUID providerId) {
        AiToolProvider provider = provider(providerId);
        provider.disable();
        return providerView(providerRepository.save(provider));
    }

    public AiProviderView addModel(UUID providerId, AiModelCommand command) {
        AiToolProvider provider = provider(providerId);
        if (modelRepository.existsByProviderIdAndModelCodeAndDeletedFalse(
                provider.getId(), AiToolProvider.normalizeCode(command.modelCode(), "modelCode"))) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "AI model code already exists for provider");
        }
        AiModel model = AiModel.create(
                provider.getId(),
                command.modelCode(),
                command.modelName(),
                command.status(),
                command.enabled(),
                command.defaultModel(),
                command.capabilities(),
                command.costMetadata(),
                command.qualityMetadata(),
                command.rateLimitMetadata());
        modelRepository.save(model);
        return providerView(provider);
    }

    public AiProviderView addCapability(UUID providerId, AiToolCapabilityCommand command) {
        AiToolProvider provider = provider(providerId);
        capabilityRepository.save(AiToolCapability.create(
                provider.getId(),
                command.capabilityCode(),
                command.layerCode(),
                command.modelCode(),
                command.enabled(),
                command.metadata()));
        return providerView(provider);
    }

    @Transactional(readOnly = true)
    public AiProviderView getProvider(UUID providerId) {
        return providerView(provider(providerId));
    }

    @Transactional(readOnly = true)
    public List<AiProviderView> listProviders() {
        return providerRepository.findAllByDeletedFalseOrderByProviderNameAsc().stream()
                .map(this::providerView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AiProviderView> listEnabledProviders() {
        return providerRepository.findAllByEnabledTrueAndStatusAndDeletedFalseOrderByProviderNameAsc(ProviderStatus.ACTIVE).stream()
                .map(this::providerView)
                .toList();
    }

    private AiProviderView providerView(AiToolProvider provider) {
        return mapper.toView(
                provider,
                modelRepository.findAllByProviderIdAndDeletedFalseOrderByModelNameAsc(provider.getId()),
                capabilityRepository.findAllByProviderIdAndDeletedFalseOrderByLayerCodeAscCapabilityCodeAsc(provider.getId()));
    }

    private AiToolProvider provider(UUID providerId) {
        return providerRepository.findById(providerId)
                .filter(provider -> !provider.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "AI provider not found"));
    }
}
