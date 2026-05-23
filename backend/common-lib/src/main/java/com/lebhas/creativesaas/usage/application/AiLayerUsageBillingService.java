package com.lebhas.creativesaas.usage.application;

import com.lebhas.ai.application.CostEfficiencyCalculator;
import com.lebhas.ai.domain.AiModel;
import com.lebhas.ai.domain.AiToolProvider;
import com.lebhas.ai.domain.LayerCostPolicy;
import com.lebhas.ai.infrastructure.persistence.AiModelRepository;
import com.lebhas.ai.infrastructure.persistence.AiToolProviderRepository;
import com.lebhas.ai.infrastructure.persistence.LayerCostPolicyRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionRepository;
import com.lebhas.creativesaas.usage.application.PlanUsagePolicyResolver.PlanUsagePolicy;
import com.lebhas.creativesaas.usage.application.dto.AiCostUsageView;
import com.lebhas.creativesaas.usage.application.dto.AiLayerUsageBillingCommand;
import com.lebhas.creativesaas.usage.domain.UsageBillingLog;
import com.lebhas.creativesaas.usage.domain.WorkspaceUsageSummary;
import com.lebhas.creativesaas.usage.event.BillingUsageLoggedEventDto;
import com.lebhas.creativesaas.usage.event.UsageBillingEventProducer;
import com.lebhas.creativesaas.usage.infrastructure.persistence.UsageBillingLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AiLayerUsageBillingService {

    private static final String USAGE_TYPE = "AI_LAYER_EXECUTION";
    private static final String REFERENCE_TYPE = "LAYER_EXECUTION_LOG";

    private final AiToolProviderRepository aiToolProviderRepository;
    private final AiModelRepository aiModelRepository;
    private final LayerCostPolicyRepository layerCostPolicyRepository;
    private final CreativeRequestRepository creativeRequestRepository;
    private final GeneratedVersionRepository generatedVersionRepository;
    private final UsageBillingLogRepository usageBillingLogRepository;
    private final WorkspaceUsageSummaryService workspaceUsageSummaryService;
    private final PlanUsagePolicyResolver planUsagePolicyResolver;
    private final CostEfficiencyCalculator costEfficiencyCalculator;
    private final AiCostUsageMapper aiCostUsageMapper;
    private final UsageBillingEventProducer usageBillingEventProducer;

    public AiLayerUsageBillingService(
            AiToolProviderRepository aiToolProviderRepository,
            AiModelRepository aiModelRepository,
            LayerCostPolicyRepository layerCostPolicyRepository,
            CreativeRequestRepository creativeRequestRepository,
            GeneratedVersionRepository generatedVersionRepository,
            UsageBillingLogRepository usageBillingLogRepository,
            WorkspaceUsageSummaryService workspaceUsageSummaryService,
            PlanUsagePolicyResolver planUsagePolicyResolver,
            CostEfficiencyCalculator costEfficiencyCalculator,
            AiCostUsageMapper aiCostUsageMapper,
            UsageBillingEventProducer usageBillingEventProducer
    ) {
        this.aiToolProviderRepository = aiToolProviderRepository;
        this.aiModelRepository = aiModelRepository;
        this.layerCostPolicyRepository = layerCostPolicyRepository;
        this.creativeRequestRepository = creativeRequestRepository;
        this.generatedVersionRepository = generatedVersionRepository;
        this.usageBillingLogRepository = usageBillingLogRepository;
        this.workspaceUsageSummaryService = workspaceUsageSummaryService;
        this.planUsagePolicyResolver = planUsagePolicyResolver;
        this.costEfficiencyCalculator = costEfficiencyCalculator;
        this.aiCostUsageMapper = aiCostUsageMapper;
        this.usageBillingEventProducer = usageBillingEventProducer;
    }

    @Transactional
    public AiCostUsageView recordLayerExecutionUsage(AiLayerUsageBillingCommand command) {
        UUID workspaceId = require(command.workspaceId(), "workspaceId");
        UUID creativeRequestId = require(command.creativeRequestId(), "creativeRequestId");
        UUID layerExecutionLogId = require(command.layerExecutionLogId(), "layerExecutionLogId");
        UUID layerId = require(command.layerId(), "layerId");
        UUID providerId = require(command.providerId(), "providerId");
        LocalDate usageMonth = normalizeMonth(command.usageMonth());

        CreativeRequestEntity creativeRequest = creativeRequestRepository.findByIdAndWorkspaceIdAndDeletedFalse(creativeRequestId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATIVE_REQUEST_FOUNDATION_NOT_FOUND));
        GeneratedVersionEntity generatedVersion = resolveGeneratedVersion(command.generatedVersionId(), workspaceId, creativeRequest.getId());
        AiToolProvider provider = aiToolProviderRepository.findByIdAndDeletedFalse(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "AI provider not found"));
        AiModel model = resolveModel(command, providerId);

        return usageBillingLogRepository.findByWorkspaceIdAndReferenceTypeAndReferenceId(workspaceId, REFERENCE_TYPE, layerExecutionLogId)
                .map(existing -> aiCostUsageMapper.toLayerView(
                        existing,
                        creativeRequestId,
                        generatedVersion == null ? null : generatedVersion.getId(),
                        layerExecutionLogId,
                        layerId,
                        provider,
                        model,
                        usageMonth,
                        normalizeUnits(command.requestedUnits())))
                .orElseGet(() -> createLayerUsageLog(command, workspaceId, creativeRequestId, generatedVersion, layerExecutionLogId, layerId, provider, model, usageMonth));
    }

    private AiCostUsageView createLayerUsageLog(
            AiLayerUsageBillingCommand command,
            UUID workspaceId,
            UUID creativeRequestId,
            GeneratedVersionEntity generatedVersion,
            UUID layerExecutionLogId,
            UUID layerId,
            AiToolProvider provider,
            AiModel model,
            LocalDate usageMonth
    ) {
        BigDecimal requestedUnits = normalizeUnits(command.requestedUnits());
        BigDecimal estimatedCostUsd = estimateLayerCost(layerId, provider, model, requestedUnits);
        PlanUsagePolicy policy = planUsagePolicyResolver.resolve(workspaceId);
        UsageBillingLog log = UsageBillingLog.create(
                workspaceId,
                USAGE_TYPE,
                REFERENCE_TYPE,
                layerExecutionLogId,
                normalizeCredits(command.creditsCharged()),
                estimatedCostUsd,
                policy.subscription().getPricingPlanId(),
                policy.featurePolicy().getId());
        UsageBillingLog saved = usageBillingLogRepository.save(log);
        publishBillingUsageLogged(saved);

        WorkspaceUsageSummary summary = workspaceUsageSummaryService.getOrCreateSummary(workspaceId, usageMonth);
        summary.recordLayerExecutionCost(estimatedCostUsd);
        workspaceUsageSummaryService.recordSummaryMutation(summary, REFERENCE_TYPE, layerExecutionLogId, "AI_LAYER_COST_TRACKED");

        return aiCostUsageMapper.toLayerView(
                saved,
                creativeRequestId,
                generatedVersion == null ? null : generatedVersion.getId(),
                layerExecutionLogId,
                layerId,
                provider,
                model,
                usageMonth,
                requestedUnits);
    }

    private void publishBillingUsageLogged(UsageBillingLog log) {
        usageBillingEventProducer.publishBillingUsageLogged(new BillingUsageLoggedEventDto(
                log.getWorkspaceId(),
                log.getId(),
                log.getUsageType(),
                log.getReferenceType(),
                log.getReferenceId(),
                log.getCreditsCharged(),
                log.getEstimatedCostUsd(),
                log.getPricingPlanId(),
                log.getPlanFeaturePolicyId(),
                java.time.Instant.now()));
    }

    private GeneratedVersionEntity resolveGeneratedVersion(UUID generatedVersionId, UUID workspaceId, UUID creativeRequestId) {
        if (generatedVersionId == null) {
            return null;
        }
        GeneratedVersionEntity generatedVersion = generatedVersionRepository.findByIdAndWorkspaceIdAndDeletedFalse(generatedVersionId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GENERATED_VERSION_NOT_FOUND));
        if (!creativeRequestId.equals(generatedVersion.getCreativeRequestId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "Generated version does not belong to the creative request");
        }
        return generatedVersion;
    }

    private AiModel resolveModel(AiLayerUsageBillingCommand command, UUID providerId) {
        if (command.modelId() != null) {
            return aiModelRepository.findByIdAndProviderIdAndDeletedFalse(command.modelId(), providerId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "AI model not found"));
        }
        String modelCode = normalizeNullable(command.modelCode());
        if (modelCode == null) {
            return null;
        }
        return aiModelRepository.findByProviderIdAndModelCodeAndDeletedFalse(providerId, modelCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "AI model not found"));
    }

    private BigDecimal estimateLayerCost(UUID layerId, AiToolProvider provider, AiModel model, BigDecimal requestedUnits) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.putAll(provider.getCostMetadata());
        if (model != null) {
            metadata.putAll(model.getCostMetadata());
        }
        layerCostPolicyRepository.findAllByPipelineLayerIdAndDeletedFalseOrderByPriorityOrderAsc(layerId).stream()
                .filter(LayerCostPolicy::isEnabled)
                .findFirst()
                .ifPresent(policy -> {
                    metadata.putAll(policy.getBudgetMetadata());
                    metadata.putAll(policy.getCostRules());
                });
        BigDecimal estimatedCostUsd = costEfficiencyCalculator.estimateCostUsd(metadata, requestedUnits);
        if (estimatedCostUsd == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "AI layer cost metadata is not configured");
        }
        return estimatedCostUsd.setScale(6, RoundingMode.HALF_UP);
    }

    private LocalDate normalizeMonth(LocalDate usageMonth) {
        if (usageMonth == null) {
            return LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        }
        return usageMonth.withDayOfMonth(1);
    }

    private BigDecimal normalizeUnits(BigDecimal units) {
        if (units == null || units.signum() <= 0) {
            return BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
        }
        return units.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeCredits(BigDecimal credits) {
        if (credits == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        if (credits.signum() < 0) {
            throw new IllegalArgumentException("creditsCharged must not be negative");
        }
        return credits.setScale(4, RoundingMode.HALF_UP);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }
}
