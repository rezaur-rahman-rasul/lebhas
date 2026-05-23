package com.lebhas.creativesaas.pricing.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.pricing.application.dto.CreatePricingPlanCommand;
import com.lebhas.creativesaas.pricing.application.dto.PricingPlanView;
import com.lebhas.creativesaas.pricing.application.dto.UpdatePricingPlanCommand;
import com.lebhas.creativesaas.pricing.cache.PricingCacheInvalidationService;
import com.lebhas.pricing.PricingPlan;
import com.lebhas.pricing.PricingPlanRepository;
import com.lebhas.pricing.WorkspaceSubscription;
import com.lebhas.pricing.WorkspaceSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PricingPlanService {

    private final CurrentUserContext currentUserContext;
    private final PricingPlanRepository pricingPlanRepository;
    private final WorkspaceSubscriptionRepository workspaceSubscriptionRepository;
    private final PricingMapper pricingMapper;
    private final PricingCacheInvalidationService pricingCacheInvalidationService;

    public PricingPlanService(
            CurrentUserContext currentUserContext,
            PricingPlanRepository pricingPlanRepository,
            WorkspaceSubscriptionRepository workspaceSubscriptionRepository,
            PricingMapper pricingMapper,
            PricingCacheInvalidationService pricingCacheInvalidationService
    ) {
        this.currentUserContext = currentUserContext;
        this.pricingPlanRepository = pricingPlanRepository;
        this.workspaceSubscriptionRepository = workspaceSubscriptionRepository;
        this.pricingMapper = pricingMapper;
        this.pricingCacheInvalidationService = pricingCacheInvalidationService;
    }

    @Transactional
    public PricingPlanView createPricingPlan(CreatePricingPlanCommand command) {
        requireMaster();
        validateUniqueCode(command.code(), null);
        PricingPlan pricingPlan = PricingPlan.create(
                command.name(),
                command.code(),
                command.description(),
                command.monthlyPrice(),
                command.yearlyPrice(),
                command.currency(),
                command.defaultPlan(),
                command.active(),
                command.sortOrder());
        pricingPlan = pricingPlanRepository.save(pricingPlan);
        List<UUID> invalidatedPlanIds = new ArrayList<>();
        invalidatedPlanIds.add(pricingPlan.getId());
        if (command.defaultPlan()) {
            invalidatedPlanIds.addAll(clearOtherDefaults(pricingPlan.getId()));
        }
        invalidatePlans(invalidatedPlanIds);
        invalidateWorkspaceSubscriptionsForPlan(pricingPlan.getId());
        return pricingMapper.toPricingPlanView(pricingPlan);
    }

    @Transactional
    public PricingPlanView updatePricingPlan(UpdatePricingPlanCommand command) {
        requireMaster();
        PricingPlan pricingPlan = requirePricingPlan(command.pricingPlanId());
        validateUniqueCode(command.code(), pricingPlan.getId());
        pricingPlan.update(
                command.name(),
                command.code(),
                command.description(),
                command.monthlyPrice(),
                command.yearlyPrice(),
                command.currency(),
                command.defaultPlan(),
                command.active(),
                command.sortOrder());
        pricingPlan = pricingPlanRepository.save(pricingPlan);
        List<UUID> invalidatedPlanIds = new ArrayList<>();
        invalidatedPlanIds.add(pricingPlan.getId());
        if (command.defaultPlan()) {
            invalidatedPlanIds.addAll(clearOtherDefaults(pricingPlan.getId()));
        }
        invalidatePlans(invalidatedPlanIds);
        return pricingMapper.toPricingPlanView(pricingPlan);
    }

    @Transactional
    public PricingPlanView disablePricingPlan(UUID pricingPlanId) {
        requireMaster();
        PricingPlan pricingPlan = requirePricingPlan(pricingPlanId);
        pricingPlan.deactivate();
        pricingPlan.markDefault(false);
        pricingPlan = pricingPlanRepository.save(pricingPlan);
        pricingCacheInvalidationService.invalidatePricingPlanDisabled(pricingPlanId);
        invalidateWorkspaceSubscriptionsForPlan(pricingPlanId);
        return pricingMapper.toPricingPlanView(pricingPlan);
    }

    @Transactional(readOnly = true)
    PricingPlan requirePricingPlan(UUID pricingPlanId) {
        return pricingPlanRepository.findByIdAndDeletedFalse(pricingPlanId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Pricing plan not found"));
    }

    private void validateUniqueCode(String code, UUID currentPlanId) {
        pricingPlanRepository.findByCodeIgnoreCaseAndDeletedFalse(code)
                .filter(existing -> !existing.getId().equals(currentPlanId))
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Pricing plan code is already in use");
                });
    }

    private List<UUID> clearOtherDefaults(UUID selectedPlanId) {
        List<UUID> changedPlanIds = new ArrayList<>();
        List<PricingPlan> plansToUpdate = pricingPlanRepository.findAllByDeletedFalseOrderBySortOrderAscNameAsc().stream()
                .filter(PricingPlan::isDefault)
                .filter(plan -> !plan.getId().equals(selectedPlanId))
                .peek(plan -> {
                    plan.markDefault(false);
                    changedPlanIds.add(plan.getId());
                })
                .toList();
        if (!plansToUpdate.isEmpty()) {
            pricingPlanRepository.saveAll(plansToUpdate);
        }
        return changedPlanIds;
    }

    private void invalidatePlans(List<UUID> planIds) {
        planIds.stream()
                .distinct()
                .forEach(pricingCacheInvalidationService::invalidatePlanRelatedCaches);
    }

    private void invalidateWorkspaceSubscriptionsForPlan(UUID pricingPlanId) {
        pricingCacheInvalidationService.invalidateWorkspaceSubscriptions(
                workspaceSubscriptionRepository.findAllByPricingPlanIdAndDeletedFalse(pricingPlanId).stream()
                        .map(WorkspaceSubscription::getWorkspaceId)
                        .toList());
    }

    private CurrentUser requireMaster() {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        if (!currentUser.isMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return currentUser;
    }
}
