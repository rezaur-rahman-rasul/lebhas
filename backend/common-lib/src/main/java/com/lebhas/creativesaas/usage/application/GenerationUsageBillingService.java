package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionRepository;
import com.lebhas.creativesaas.usage.application.PlanUsagePolicyResolver.PlanUsagePolicy;
import com.lebhas.creativesaas.usage.application.dto.AiCostUsageView;
import com.lebhas.creativesaas.usage.application.dto.GenerationUsageBillingCommand;
import com.lebhas.creativesaas.usage.domain.UsageBillingLog;
import com.lebhas.creativesaas.usage.event.BillingUsageLoggedEventDto;
import com.lebhas.creativesaas.usage.event.UsageBillingEventProducer;
import com.lebhas.creativesaas.usage.infrastructure.persistence.UsageBillingLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class GenerationUsageBillingService {

    private static final String USAGE_TYPE = "AI_GENERATION_COST";
    private static final String REFERENCE_TYPE = "GENERATED_VERSION";

    private final CreativeRequestRepository creativeRequestRepository;
    private final GeneratedVersionRepository generatedVersionRepository;
    private final UsageBillingLogRepository usageBillingLogRepository;
    private final PlanUsagePolicyResolver planUsagePolicyResolver;
    private final AiCostUsageMapper aiCostUsageMapper;
    private final UsageBillingEventProducer usageBillingEventProducer;

    public GenerationUsageBillingService(
            CreativeRequestRepository creativeRequestRepository,
            GeneratedVersionRepository generatedVersionRepository,
            UsageBillingLogRepository usageBillingLogRepository,
            PlanUsagePolicyResolver planUsagePolicyResolver,
            AiCostUsageMapper aiCostUsageMapper,
            UsageBillingEventProducer usageBillingEventProducer
    ) {
        this.creativeRequestRepository = creativeRequestRepository;
        this.generatedVersionRepository = generatedVersionRepository;
        this.usageBillingLogRepository = usageBillingLogRepository;
        this.planUsagePolicyResolver = planUsagePolicyResolver;
        this.aiCostUsageMapper = aiCostUsageMapper;
        this.usageBillingEventProducer = usageBillingEventProducer;
    }

    @Transactional
    public AiCostUsageView recordGeneratedVersionUsage(GenerationUsageBillingCommand command) {
        UUID workspaceId = require(command.workspaceId(), "workspaceId");
        UUID creativeRequestId = require(command.creativeRequestId(), "creativeRequestId");
        UUID generatedVersionId = require(command.generatedVersionId(), "generatedVersionId");
        LocalDate usageMonth = normalizeMonth(command.usageMonth());

        CreativeRequestEntity creativeRequest = creativeRequestRepository.findByIdAndWorkspaceIdAndDeletedFalse(creativeRequestId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATIVE_REQUEST_FOUNDATION_NOT_FOUND));
        GeneratedVersionEntity generatedVersion = generatedVersionRepository.findByIdAndWorkspaceIdAndDeletedFalse(generatedVersionId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GENERATED_VERSION_NOT_FOUND));
        if (!creativeRequest.getId().equals(generatedVersion.getCreativeRequestId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "Generated version does not belong to the creative request");
        }

        return usageBillingLogRepository.findByWorkspaceIdAndReferenceTypeAndReferenceId(workspaceId, REFERENCE_TYPE, generatedVersionId)
                .map(existing -> aiCostUsageMapper.toGenerationView(existing, creativeRequestId, generatedVersionId, usageMonth))
                .orElseGet(() -> createGenerationUsageLog(command, workspaceId, creativeRequestId, generatedVersionId, usageMonth));
    }

    private AiCostUsageView createGenerationUsageLog(
            GenerationUsageBillingCommand command,
            UUID workspaceId,
            UUID creativeRequestId,
            UUID generatedVersionId,
            LocalDate usageMonth
    ) {
        BigDecimal estimatedCostUsd = normalizeCost(command.estimatedCostUsd());
        PlanUsagePolicy policy = planUsagePolicyResolver.resolve(workspaceId);
        UsageBillingLog log = UsageBillingLog.create(
                workspaceId,
                USAGE_TYPE,
                REFERENCE_TYPE,
                generatedVersionId,
                normalizeCredits(command.creditsCharged()),
                estimatedCostUsd,
                policy.subscription().getPricingPlanId(),
                policy.featurePolicy().getId());
        UsageBillingLog saved = usageBillingLogRepository.save(log);
        publishBillingUsageLogged(saved);
        return aiCostUsageMapper.toGenerationView(saved, creativeRequestId, generatedVersionId, usageMonth);
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

    private LocalDate normalizeMonth(LocalDate usageMonth) {
        if (usageMonth == null) {
            return LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        }
        return usageMonth.withDayOfMonth(1);
    }

    private BigDecimal normalizeCost(BigDecimal cost) {
        if (cost == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Generation AI cost must come from recorded layer usage");
        }
        if (cost.signum() < 0) {
            throw new IllegalArgumentException("estimatedCostUsd must not be negative");
        }
        return cost.setScale(6, RoundingMode.HALF_UP);
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

    private <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }
}
