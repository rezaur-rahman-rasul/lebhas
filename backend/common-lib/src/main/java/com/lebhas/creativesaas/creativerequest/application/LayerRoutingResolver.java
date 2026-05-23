package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.ai.cache.AiRoutingDecisionCacheService;
import com.lebhas.ai.cache.RoutingDecisionCacheEntry;
import com.lebhas.ai.domain.CreativePipelineLayer;
import com.lebhas.ai.domain.LayerRoutingPolicy;
import com.lebhas.ai.domain.LayerRoutingStrategy;
import com.lebhas.ai.infrastructure.persistence.LayerRoutingPolicyRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LayerRoutingResolver {

    private final LayerRoutingPolicyRepository layerRoutingPolicyRepository;
    private final LayerToolResolver layerToolResolver;
    private final CostAwareRoutingService costAwareRoutingService;
    private final QualityAwareRoutingService qualityAwareRoutingService;
    private final AiRoutingDecisionCacheService aiRoutingDecisionCacheService;

    public LayerRoutingResolver(
            LayerRoutingPolicyRepository layerRoutingPolicyRepository,
            LayerToolResolver layerToolResolver,
            CostAwareRoutingService costAwareRoutingService,
            QualityAwareRoutingService qualityAwareRoutingService,
            AiRoutingDecisionCacheService aiRoutingDecisionCacheService
    ) {
        this.layerRoutingPolicyRepository = layerRoutingPolicyRepository;
        this.layerToolResolver = layerToolResolver;
        this.costAwareRoutingService = costAwareRoutingService;
        this.qualityAwareRoutingService = qualityAwareRoutingService;
        this.aiRoutingDecisionCacheService = aiRoutingDecisionCacheService;
    }

    @Transactional(readOnly = true)
    public LayerRoutingDecision resolve(PipelineResolutionContext context, CreativePipelineLayer layer, CreativeRequestEntity request) {
        List<LayerToolCandidate> candidates = layerToolResolver.resolveCandidates(layer);
        if (candidates.isEmpty() && layer.isRequired()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Required creative pipeline layer has no eligible tool mapping");
        }
        Optional<LayerRoutingPolicy> routingPolicy = activeRoutingPolicy(layer);
        Optional<com.lebhas.ai.domain.LayerCostPolicy> costPolicy = costAwareRoutingService.activePolicy(layer.getId());
        Optional<com.lebhas.ai.domain.LayerQualityPolicy> qualityPolicy = qualityAwareRoutingService.activePolicy(layer.getId());
        LayerRoutingStrategy strategy = routingPolicy.map(LayerRoutingPolicy::getRoutingStrategy).orElse(LayerRoutingStrategy.PRIORITY);
        LayerToolCandidate candidate = chooseCandidate(strategy, candidates, costPolicy, qualityPolicy);
        BigDecimal estimatedCost = costAwareRoutingService.estimateLayerCost(candidate, costPolicy);
        BigDecimal qualityScore = qualityAwareRoutingService.estimateQualityScore(candidate);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("strategy", strategy.name());
        metadata.put("policyCode", routingPolicy.map(LayerRoutingPolicy::getPolicyCode).orElse(null));
        metadata.put("candidateCount", candidates.size());
        aiRoutingDecisionCacheService.store(new RoutingDecisionCacheEntry(
                request.getWorkspaceId(),
                layer.getLayerType(),
                context.pipeline().getId(),
                layer.getId(),
                candidate == null ? null : candidate.provider().getId(),
                candidate == null ? null : candidate.mapping().getModelId(),
                candidate == null ? null : candidate.mapping().getCapabilityId(),
                strategy.name(),
                metadata,
                Instant.now()));
        return new LayerRoutingDecision(layer, routingPolicy, costPolicy, qualityPolicy, candidate, estimatedCost, qualityScore, metadata);
    }

    private Optional<LayerRoutingPolicy> activeRoutingPolicy(CreativePipelineLayer layer) {
        return layerRoutingPolicyRepository.findAllByPipelineLayerIdAndDeletedFalseOrderByPriorityOrderAsc(layer.getId())
                .stream()
                .filter(LayerRoutingPolicy::isEnabled)
                .findFirst();
    }

    private LayerToolCandidate chooseCandidate(
            LayerRoutingStrategy strategy,
            List<LayerToolCandidate> candidates,
            Optional<com.lebhas.ai.domain.LayerCostPolicy> costPolicy,
            Optional<com.lebhas.ai.domain.LayerQualityPolicy> qualityPolicy
    ) {
        if (candidates.isEmpty()) {
            return null;
        }
        return switch (strategy) {
            case COST_OPTIMIZED -> costAwareRoutingService.chooseLowestCost(candidates, costPolicy).orElse(candidates.get(0));
            case QUALITY_OPTIMIZED -> qualityAwareRoutingService.chooseHighestQuality(candidates, qualityPolicy).orElse(candidates.get(0));
            case WEIGHTED -> candidates.stream()
                    .max(java.util.Comparator.comparing(candidate -> candidate.mapping().getRoutingWeight()))
                    .orElse(candidates.get(0));
            case PRIORITY, FALLBACK_CHAIN, MANUAL_RULE -> candidates.get(0);
        };
    }
}
