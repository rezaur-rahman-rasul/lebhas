package com.lebhas.creativesaas.texttool.application;

import com.lebhas.ai.application.MasterAiProviderToolRegistryService;
import com.lebhas.ai.application.dto.ResolvedProviderRouteView;
import com.lebhas.ai.domain.CreativeTool;
import com.lebhas.ai.domain.ToolCreditCostPolicy;
import com.lebhas.ai.infrastructure.persistence.CreativeToolRepository;
import com.lebhas.ai.infrastructure.persistence.ToolCreditCostPolicyRepository;
import com.lebhas.creativesaas.brand.domain.BrandEntity;
import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.brand.infrastructure.persistence.BrandRepository;
import com.lebhas.creativesaas.common.api.PagedResult;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.generation.application.CreativeCreditReservationService;
import com.lebhas.creativesaas.generation.application.dto.CreditFinalizeCommand;
import com.lebhas.creativesaas.generation.application.dto.CreditRefundCommand;
import com.lebhas.creativesaas.generation.application.dto.CreditReservationResult;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.pricing.application.WorkspacePlanContextService;
import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspacePlanContextView;
import com.lebhas.creativesaas.product.domain.ProductServiceEntity;
import com.lebhas.creativesaas.product.infrastructure.persistence.ProductServiceRepository;
import com.lebhas.creativesaas.project.domain.ProjectEntity;
import com.lebhas.creativesaas.project.infrastructure.persistence.ProjectRepository;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.texttool.application.dto.CreativeTextToolCommand;
import com.lebhas.creativesaas.texttool.application.dto.CreativeTextToolHistoryView;
import com.lebhas.creativesaas.texttool.application.dto.CreativeTextToolOutputView;
import com.lebhas.creativesaas.texttool.application.dto.CreativeTextToolRequest;
import com.lebhas.creativesaas.texttool.domain.CreativeTextQualityMode;
import com.lebhas.creativesaas.texttool.domain.CreativeTextToolHistory;
import com.lebhas.creativesaas.texttool.domain.CreativeTextToolOutput;
import com.lebhas.creativesaas.texttool.domain.CreativeTextToolType;
import com.lebhas.creativesaas.texttool.infrastructure.persistence.CreativeTextToolHistoryRepository;
import com.lebhas.creativesaas.texttool.infrastructure.persistence.CreativeTextToolOutputRepository;
import com.lebhas.creativesaas.usage.domain.UsageBillingLog;
import com.lebhas.creativesaas.usage.infrastructure.persistence.UsageBillingLogRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CreativeTextToolService {

    private static final String REFERENCE_TYPE = "creative_text_tool_output";

    private final ProjectRepository projectRepository;
    private final BrandRepository brandRepository;
    private final ProductServiceRepository productServiceRepository;
    private final CreativeToolRepository creativeToolRepository;
    private final ToolCreditCostPolicyRepository costPolicyRepository;
    private final WorkspacePlanContextService workspacePlanContextService;
    private final MasterAiProviderToolRegistryService providerToolRegistryService;
    private final CreativeCreditReservationService creditReservationService;
    private final ObjectProvider<CreativeTextToolProvider> provider;
    private final CreativeTextToolOutputRepository outputRepository;
    private final CreativeTextToolHistoryRepository historyRepository;
    private final UsageBillingLogRepository usageBillingLogRepository;
    private final CreativeTextToolMapper mapper;
    private DomainEventPublisher domainEventPublisher;
    private CurrentUserContext currentUserContext;

    public CreativeTextToolService(
            ProjectRepository projectRepository,
            BrandRepository brandRepository,
            ProductServiceRepository productServiceRepository,
            CreativeToolRepository creativeToolRepository,
            ToolCreditCostPolicyRepository costPolicyRepository,
            WorkspacePlanContextService workspacePlanContextService,
            MasterAiProviderToolRegistryService providerToolRegistryService,
            CreativeCreditReservationService creditReservationService,
            ObjectProvider<CreativeTextToolProvider> provider,
            CreativeTextToolOutputRepository outputRepository,
            CreativeTextToolHistoryRepository historyRepository,
            UsageBillingLogRepository usageBillingLogRepository,
            CreativeTextToolMapper mapper
    ) {
        this.projectRepository = projectRepository;
        this.brandRepository = brandRepository;
        this.productServiceRepository = productServiceRepository;
        this.creativeToolRepository = creativeToolRepository;
        this.costPolicyRepository = costPolicyRepository;
        this.workspacePlanContextService = workspacePlanContextService;
        this.providerToolRegistryService = providerToolRegistryService;
        this.creditReservationService = creditReservationService;
        this.provider = provider;
        this.outputRepository = outputRepository;
        this.historyRepository = historyRepository;
        this.usageBillingLogRepository = usageBillingLogRepository;
        this.mapper = mapper;
    }

    @Autowired(required = false)
    void setDomainEventPublisher(DomainEventPublisher domainEventPublisher) {
        this.domainEventPublisher = domainEventPublisher;
    }

    @Autowired(required = false)
    void setCurrentUserContext(CurrentUserContext currentUserContext) {
        this.currentUserContext = currentUserContext;
    }

    @Transactional
    public CreativeTextToolOutputView generate(CreativeTextToolCommand command) {
        UUID workspaceId = require(command.workspaceId(), "workspaceId");
        UUID projectId = require(command.projectId(), "projectId");
        CreativeTextToolType toolType = require(command.toolType(), "toolType");
        CreativeTextToolRequest request = require(command.request(), "request");

        ProjectEntity project = projectRepository.findByIdAndWorkspaceIdAndDeletedFalse(projectId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Project not found"));
        BrandEntity brand = resolveBrand(workspaceId, project, request.brandId());
        ProductServiceEntity product = resolveProduct(workspaceId, brand.getId(), request.productServiceId());
        validateLanguage(brand, request.language());

        CreativeTool tool = creativeToolRepository.findByToolCodeAndDeletedFalse(toolType.toolCode())
                .filter(CreativeTool::isEnabled)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Creative text tool is not configured"));
        WorkspacePlanContextView planContext = workspacePlanContextService.getWorkspacePlanContext(workspaceId);
        requirePackageAllowsTool(planContext.featurePolicy(), tool.getToolCode(), qualityMode(request));
        BigDecimal creditCost = resolveCreditCost(tool.getId());

        Map<String, Object> requestPayload = requestPayload(project, brand, product, toolType, tool.getToolCode(), request);
        CreativeTextToolHistory history = historyRepository.save(CreativeTextToolHistory.requested(
                workspaceId,
                projectId,
                toolType,
                tool.getToolCode(),
                creditCost,
                requestPayload));
        publish(KafkaTopicConstants.TEXT_TOOL_GENERATION_REQUESTED, workspaceId, history.getId(), requestPayload);

        CreditReservationResult reservation = null;
        try {
            if (creditCost.signum() > 0) {
                reservation = creditReservationService.reserveCredits(workspaceId, creditCost, REFERENCE_TYPE, history.getId());
                publish(KafkaTopicConstants.CREDITS_RESERVED, workspaceId, history.getId(), Map.of(
                        "creditCost", creditCost,
                        "reservationId", reservation.reservationId().toString()));
            }

            ResolvedProviderRouteView route = providerToolRegistryService.resolveProvider(tool.getId(), qualityMode(request).name());
            CreativeTextToolProvider generationProvider = provider.getIfAvailable(DeterministicCreativeTextToolProvider::new);
            Map<String, Object> outputPayload = generationProvider.generate(new TextToolGenerationContext(
                    toolType,
                    tool.getToolCode(),
                    project,
                    brand,
                    product,
                    request));
            CreativeTextToolOutput output = outputRepository.save(CreativeTextToolOutput.create(
                    workspaceId,
                    projectId,
                    brand.getId(),
                    product == null ? null : product.getId(),
                    toolType,
                    tool.getToolCode(),
                    qualityMode(request),
                    require(request.platform(), "platform"),
                    require(request.language(), "language"),
                    request.tone(),
                    request.campaignObjective() == null ? project.getCampaignObjective() : request.campaignObjective(),
                    request.sourceIdea(),
                    route.providerId(),
                    route.modelId(),
                    creditCost,
                    reservation == null ? null : reservation.reservationId(),
                    Map.of("ids", selectedAssetIds(request)),
                    outputPayload));

            history.complete(output.getId(), outputPayload);
            historyRepository.save(history);
            if (reservation != null) {
                creditReservationService.finalizeCredits(new CreditFinalizeCommand(workspaceId, reservation.reservationId(), REFERENCE_TYPE, history.getId(), "text_tool_generation_completed"));
                publish(KafkaTopicConstants.CREDITS_FINALIZED, workspaceId, history.getId(), Map.of("creditCost", creditCost));
            }
            UsageBillingLog usageLog = usageBillingLogRepository.save(UsageBillingLog.create(
                    workspaceId,
                    "TEXT_TOOL_" + toolType.name(),
                    REFERENCE_TYPE,
                    output.getId(),
                    creditCost,
                    null,
                    planContext.pricingPlan() == null ? null : planContext.pricingPlan().id(),
                    planContext.featurePolicy() == null ? null : planContext.featurePolicy().id()));
            publish(KafkaTopicConstants.USAGE_EVENT_RECORDED, workspaceId, usageLog.getId(), Map.of(
                    "usageType", usageLog.getUsageType(),
                    "referenceId", output.getId().toString()));
            publish(KafkaTopicConstants.TEXT_TOOL_GENERATION_COMPLETED, workspaceId, output.getId(), Map.of(
                    "toolCode", tool.getToolCode(),
                    "historyId", history.getId().toString()));
            return mapper.toView(output);
        } catch (RuntimeException ex) {
            history.fail(ex.getMessage());
            historyRepository.save(history);
            if (reservation != null) {
                creditReservationService.refundCredits(new CreditRefundCommand(workspaceId, reservation.reservationId(), REFERENCE_TYPE, history.getId(), "text_tool_generation_failed"));
                publish(KafkaTopicConstants.CREDITS_REFUNDED, workspaceId, history.getId(), Map.of("creditCost", creditCost));
            }
            publish(KafkaTopicConstants.TEXT_TOOL_GENERATION_FAILED, workspaceId, history.getId(), Map.of(
                    "toolCode", tool.getToolCode(),
                    "reason", ex.getMessage() == null ? "generation_failed" : ex.getMessage()));
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public PagedResult<CreativeTextToolHistoryView> history(UUID workspaceId, UUID projectId, Pageable pageable) {
        projectRepository.findByIdAndWorkspaceIdAndDeletedFalse(projectId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Project not found"));
        return PagedResult.from(historyRepository
                .findAllByWorkspaceIdAndProjectIdAndDeletedFalseOrderByCreatedAtDesc(workspaceId, projectId, pageable)
                .map(mapper::toView));
    }

    @Transactional(readOnly = true)
    public CreativeTextToolOutputView getOutput(UUID workspaceId, UUID outputId) {
        return outputRepository.findByIdAndWorkspaceIdAndDeletedFalse(outputId, workspaceId)
                .map(mapper::toView)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Creative text tool output not found"));
    }

    private BrandEntity resolveBrand(UUID workspaceId, ProjectEntity project, UUID requestedBrandId) {
        UUID brandId = requestedBrandId == null ? project.getBrandId() : requestedBrandId;
        if (!project.getBrandId().equals(brandId)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Brand does not belong to the project");
        }
        return brandRepository.findByIdAndWorkspaceIdAndDeletedFalse(brandId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Brand not found"));
    }

    private ProductServiceEntity resolveProduct(UUID workspaceId, UUID brandId, UUID productServiceId) {
        if (productServiceId != null) {
            ProductServiceEntity product = productServiceRepository.findByIdAndWorkspaceIdAndDeletedFalse(productServiceId, workspaceId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Product/service not found"));
            if (!brandId.equals(product.getBrandId())) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Product/service does not belong to the brand");
            }
            return product;
        }
        List<ProductServiceEntity> products = productServiceRepository.findAllByWorkspaceIdAndBrandIdAndDeletedFalseOrderByCreatedAtDesc(workspaceId, brandId);
        return products.size() == 1 ? products.getFirst() : null;
    }

    private void validateLanguage(BrandEntity brand, PromptLanguage language) {
        PromptLanguage requested = require(language, "language");
        BrandLanguagePreference preference = brand.getLanguagePreference();
        boolean allowed = preference == BrandLanguagePreference.BOTH
                || (preference == BrandLanguagePreference.BANGLA && requested == PromptLanguage.BANGLA)
                || (preference == BrandLanguagePreference.ENGLISH && requested == PromptLanguage.ENGLISH);
        if (!allowed) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Language does not match brand language preference");
        }
    }

    private void requirePackageAllowsTool(PlanFeaturePolicyView policy, String toolCode, CreativeTextQualityMode qualityMode) {
        if (policy == null || !policy.creativeGenerationEnabled()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Package does not allow creative text tools");
        }
        Set<String> enabledCodes = policy.enabledCreativeToolCodes();
        if (enabledCodes == null || !enabledCodes.contains(toolCode)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Package does not allow selected text tool");
        }
        if (qualityMode == CreativeTextQualityMode.PREMIUM && !policy.premiumQualityEnabled()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Package does not allow premium text generation");
        }
    }

    private BigDecimal resolveCreditCost(UUID toolId) {
        Instant now = Instant.now();
        return costPolicyRepository.findFirstByToolIdAndEnabledTrueAndDeletedFalseOrderByUpdatedAtDesc(toolId)
                .filter(policy -> isEffective(policy, now))
                .map(ToolCreditCostPolicy::getCreditCost)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Master tool credit cost policy is not configured"));
    }

    private boolean isEffective(ToolCreditCostPolicy policy, Instant now) {
        return (policy.getEffectiveFrom() == null || !policy.getEffectiveFrom().isAfter(now))
                && (policy.getEffectiveUntil() == null || policy.getEffectiveUntil().isAfter(now));
    }

    private CreativeTextQualityMode qualityMode(CreativeTextToolRequest request) {
        return request.qualityMode() == null ? CreativeTextQualityMode.BASIC : request.qualityMode();
    }

    private List<String> selectedAssetIds(CreativeTextToolRequest request) {
        return request.selectedAssetIds() == null ? List.of() : request.selectedAssetIds().stream().map(UUID::toString).toList();
    }

    private Map<String, Object> requestPayload(ProjectEntity project, BrandEntity brand, ProductServiceEntity product, CreativeTextToolType toolType, String toolCode, CreativeTextToolRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("toolType", toolType.name());
        payload.put("toolCode", toolCode);
        payload.put("projectName", project.getName());
        payload.put("brandId", brand.getId().toString());
        payload.put("brandName", brand.getName());
        payload.put("productServiceId", product == null ? null : product.getId().toString());
        payload.put("platform", require(request.platform(), "platform").name());
        payload.put("language", require(request.language(), "language").name());
        payload.put("tone", request.tone());
        payload.put("campaignObjective", request.campaignObjective() == null ? null : request.campaignObjective().name());
        payload.put("sourceIdea", request.sourceIdea());
        payload.put("qualityMode", qualityMode(request).name());
        payload.put("selectedAssetIds", selectedAssetIds(request));
        payload.put("requestedBy", currentUserContext == null
                ? null
                : currentUserContext.getCurrentUser().map(user -> user.userId().toString()).orElse(null));
        return payload;
    }

    private void publish(String topic, UUID workspaceId, UUID aggregateId, Map<String, Object> attributes) {
        if (domainEventPublisher != null) {
            domainEventPublisher.publish(topic, new BaseDomainEvent(topic, workspaceId, aggregateId, Instant.now(), attributes));
        }
    }

    private <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }
}
