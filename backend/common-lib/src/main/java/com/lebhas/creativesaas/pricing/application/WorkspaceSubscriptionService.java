package com.lebhas.creativesaas.pricing.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.pricing.application.dto.AssignWorkspaceSubscriptionCommand;
import com.lebhas.creativesaas.pricing.application.dto.WorkspaceSubscriptionView;
import com.lebhas.creativesaas.pricing.cache.PricingCacheInvalidationService;
import com.lebhas.creativesaas.pricing.cache.WorkspaceSubscriptionCacheService;
import com.lebhas.creativesaas.pricing.cache.dto.WorkspaceSubscriptionCacheEntry;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.WorkspaceRepository;
import com.lebhas.pricing.PricingPlan;
import com.lebhas.pricing.WorkspaceSubscription;
import com.lebhas.pricing.WorkspaceSubscriptionRepository;
import com.lebhas.pricing.WorkspaceSubscriptionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class WorkspaceSubscriptionService {

    private final CurrentUserContext currentUserContext;
    private final WorkspaceRepository workspaceRepository;
    private final PricingPlanService pricingPlanService;
    private final WorkspaceSubscriptionRepository workspaceSubscriptionRepository;
    private final WorkspaceSubscriptionCacheService workspaceSubscriptionCacheService;
    private final PricingCacheInvalidationService pricingCacheInvalidationService;
    private final PricingMapper pricingMapper;

    public WorkspaceSubscriptionService(
            CurrentUserContext currentUserContext,
            WorkspaceRepository workspaceRepository,
            PricingPlanService pricingPlanService,
            WorkspaceSubscriptionRepository workspaceSubscriptionRepository,
            WorkspaceSubscriptionCacheService workspaceSubscriptionCacheService,
            PricingCacheInvalidationService pricingCacheInvalidationService,
            PricingMapper pricingMapper
    ) {
        this.currentUserContext = currentUserContext;
        this.workspaceRepository = workspaceRepository;
        this.pricingPlanService = pricingPlanService;
        this.workspaceSubscriptionRepository = workspaceSubscriptionRepository;
        this.workspaceSubscriptionCacheService = workspaceSubscriptionCacheService;
        this.pricingCacheInvalidationService = pricingCacheInvalidationService;
        this.pricingMapper = pricingMapper;
    }

    @Transactional
    public WorkspaceSubscriptionView assignOrChangeWorkspaceSubscription(AssignWorkspaceSubscriptionCommand command) {
        requireMaster();
        requireWorkspace(command.workspaceId());
        PricingPlan pricingPlan = pricingPlanService.requirePricingPlan(command.pricingPlanId());
        if (!pricingPlan.isActive()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Inactive pricing plans cannot be assigned");
        }
        Instant startedAt = command.startedAt() == null ? Instant.now() : command.startedAt();
        WorkspaceSubscriptionStatus status = command.status() == null
                ? WorkspaceSubscriptionStatus.ACTIVE
                : command.status();
        WorkspaceSubscription subscription = workspaceSubscriptionRepository.findFirstByWorkspaceIdAndDeletedFalse(command.workspaceId())
                .orElseGet(() -> WorkspaceSubscription.create(
                        command.workspaceId(),
                        pricingPlan.getId(),
                        status,
                        startedAt,
                        command.expiresAt(),
                        command.trialEndsAt(),
                        command.autoRenew()));
        if (subscription.getId() != null) {
            subscription.update(
                    pricingPlan.getId(),
                    status,
                    startedAt,
                    command.expiresAt(),
                    command.trialEndsAt(),
                    command.autoRenew());
        }
        subscription = workspaceSubscriptionRepository.save(subscription);
        pricingCacheInvalidationService.invalidateWorkspaceSubscriptionChanged(command.workspaceId());
        return pricingMapper.toWorkspaceSubscriptionView(subscription);
    }

    @Transactional(readOnly = true)
    public Optional<WorkspaceSubscriptionView> getWorkspaceSubscriptionForMaster(UUID workspaceId) {
        requireMaster();
        requireWorkspace(workspaceId);
        return findWorkspaceSubscriptionView(workspaceId);
    }

    @Transactional(readOnly = true)
    Optional<WorkspaceSubscriptionView> findWorkspaceSubscriptionView(UUID workspaceId) {
        WorkspaceSubscriptionCacheEntry cached = workspaceSubscriptionCacheService.getOrLoad(
                workspaceId,
                () -> workspaceSubscriptionRepository.findFirstByWorkspaceIdAndDeletedFalse(workspaceId)
                        .map(WorkspaceSubscriptionCacheEntry::from)
                        .orElse(null));
        if (cached == null) {
            return Optional.empty();
        }
        return Optional.of(pricingMapper.toWorkspaceSubscriptionView(cached));
    }

    @Transactional(readOnly = true)
    Optional<WorkspaceSubscriptionView> findActiveWorkspaceSubscriptionView(UUID workspaceId) {
        return findWorkspaceSubscriptionView(workspaceId)
                .filter(subscription -> isActiveForWorkspaceContext(subscription.status()));
    }

    @Transactional(readOnly = true)
    WorkspaceSubscription requireWorkspaceSubscription(UUID workspaceId) {
        return workspaceSubscriptionRepository.findFirstByWorkspaceIdAndDeletedFalse(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Workspace subscription not found"));
    }

    private boolean isActiveForWorkspaceContext(WorkspaceSubscriptionStatus status) {
        return status == WorkspaceSubscriptionStatus.ACTIVE
                || status == WorkspaceSubscriptionStatus.TRIAL;
    }

    private void requireWorkspace(UUID workspaceId) {
        workspaceRepository.findByIdAndDeletedFalse(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND));
    }

    private CurrentUser requireMaster() {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        if (!currentUser.isMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return currentUser;
    }
}
