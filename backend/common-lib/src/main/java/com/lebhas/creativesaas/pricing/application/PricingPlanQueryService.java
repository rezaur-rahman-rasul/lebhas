package com.lebhas.creativesaas.pricing.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.PricingPlanDetailView;
import com.lebhas.creativesaas.pricing.application.dto.PricingPlanView;
import com.lebhas.creativesaas.pricing.cache.PricingPlanCacheService;
import com.lebhas.creativesaas.pricing.cache.dto.ActivePricingPlansCacheEntry;
import com.lebhas.creativesaas.pricing.cache.dto.PricingPlanCacheEntry;
import com.lebhas.pricing.PricingPlan;
import com.lebhas.pricing.PricingPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PricingPlanQueryService {

    private final CurrentUserContext currentUserContext;
    private final PricingPlanRepository pricingPlanRepository;
    private final PricingPlanCacheService pricingPlanCacheService;
    private final PlanFeaturePolicyService planFeaturePolicyService;
    private final PricingMapper pricingMapper;

    public PricingPlanQueryService(
            CurrentUserContext currentUserContext,
            PricingPlanRepository pricingPlanRepository,
            PricingPlanCacheService pricingPlanCacheService,
            PlanFeaturePolicyService planFeaturePolicyService,
            PricingMapper pricingMapper
    ) {
        this.currentUserContext = currentUserContext;
        this.pricingPlanRepository = pricingPlanRepository;
        this.pricingPlanCacheService = pricingPlanCacheService;
        this.planFeaturePolicyService = planFeaturePolicyService;
        this.pricingMapper = pricingMapper;
    }

    @Transactional(readOnly = true)
    public List<PricingPlanDetailView> listActivePricingPlans() {
        ActivePricingPlansCacheEntry cached = pricingPlanCacheService.getActivePlansOrLoad(
                () -> new ActivePricingPlansCacheEntry(
                        pricingPlanRepository.findAllByActiveTrueAndDeletedFalseOrderBySortOrderAscNameAsc().stream()
                                .map(PricingPlanCacheEntry::from)
                                .toList(),
                        Instant.now()));
        return cached.plans().stream()
                .map(pricingMapper::toPricingPlanView)
                .map(this::toDetailView)
                .toList();
    }

    @Transactional(readOnly = true)
    public PricingPlanDetailView getActivePricingPlan(UUID pricingPlanId) {
        PricingPlanView pricingPlan = findPricingPlanView(pricingPlanId)
                .filter(PricingPlanView::active)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Active pricing plan not found"));
        return toDetailView(pricingPlan);
    }

    @Transactional(readOnly = true)
    public List<PricingPlanDetailView> listAllPricingPlansForMaster() {
        requireMaster();
        return pricingPlanRepository.findAllByDeletedFalseOrderBySortOrderAscNameAsc().stream()
                .map(pricingMapper::toPricingPlanView)
                .map(this::toDetailView)
                .toList();
    }

    @Transactional(readOnly = true)
    public PricingPlanDetailView getPricingPlanForMaster(UUID pricingPlanId) {
        requireMaster();
        PricingPlanView pricingPlan = findPricingPlanView(pricingPlanId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Pricing plan not found"));
        return toDetailView(pricingPlan);
    }

    @Transactional(readOnly = true)
    Optional<PricingPlanView> findPricingPlanView(UUID pricingPlanId) {
        PricingPlanCacheEntry cached = pricingPlanCacheService.getOrLoad(
                pricingPlanId,
                () -> pricingPlanRepository.findByIdAndDeletedFalse(pricingPlanId)
                        .map(PricingPlanCacheEntry::from)
                        .orElse(null));
        if (cached == null) {
            return Optional.empty();
        }
        return Optional.of(pricingMapper.toPricingPlanView(cached));
    }

    @Transactional(readOnly = true)
    Optional<PricingPlanView> findDefaultActivePricingPlanView() {
        ActivePricingPlansCacheEntry cached = pricingPlanCacheService.getActivePlansOrLoad(
                () -> new ActivePricingPlansCacheEntry(
                        pricingPlanRepository.findAllByActiveTrueAndDeletedFalseOrderBySortOrderAscNameAsc().stream()
                                .map(PricingPlanCacheEntry::from)
                                .toList(),
                        Instant.now()));
        return cached.plans().stream()
                .map(pricingMapper::toPricingPlanView)
                .filter(PricingPlanView::defaultPlan)
                .findFirst();
    }

    private PricingPlanDetailView toDetailView(PricingPlanView pricingPlan) {
        PlanFeaturePolicyView featurePolicy = planFeaturePolicyService.findFeaturePolicyView(pricingPlan.id()).orElse(null);
        return pricingMapper.toPricingPlanDetailView(pricingPlan, featurePolicy);
    }

    private CurrentUser requireMaster() {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        if (!currentUser.isMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return currentUser;
    }
}
