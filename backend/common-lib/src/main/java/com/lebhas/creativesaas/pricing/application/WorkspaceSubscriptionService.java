package com.lebhas.creativesaas.pricing.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.auditlog.application.AuditLogService;
import com.lebhas.creativesaas.auditlog.domain.AuditActionType;
import com.lebhas.creativesaas.auditlog.domain.AuditOutcome;
import com.lebhas.creativesaas.pricing.application.dto.AssignWorkspaceSubscriptionCommand;
import com.lebhas.creativesaas.pricing.application.dto.WorkspaceSubscriptionView;
import com.lebhas.creativesaas.pricing.cache.PricingCacheInvalidationService;
import com.lebhas.creativesaas.pricing.cache.WorkspaceSubscriptionCacheService;
import com.lebhas.creativesaas.pricing.cache.dto.WorkspaceSubscriptionCacheEntry;
import com.lebhas.creativesaas.credit.application.CreditWalletService;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.WorkspaceRepository;
import com.lebhas.pricing.PricingPlan;
import com.lebhas.pricing.WorkspaceSubscription;
import com.lebhas.pricing.WorkspaceSubscriptionRepository;
import com.lebhas.pricing.WorkspaceSubscriptionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
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
    private DomainEventPublisher domainEventPublisher;
    private CreditWalletService creditWalletService;
    private AuditLogService auditLogService;

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

    @Autowired(required = false)
    void setDomainEventPublisher(DomainEventPublisher domainEventPublisher) {
        this.domainEventPublisher = domainEventPublisher;
    }

    @Autowired(required = false)
    void setCreditWalletService(CreditWalletService creditWalletService) {
        this.creditWalletService = creditWalletService;
    }

    @Autowired(required = false)
    void setAuditLogService(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
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
        if (creditWalletService != null) {
            creditWalletService.initializeWallet(command.workspaceId());
        }
        pricingCacheInvalidationService.invalidateWorkspaceSubscriptionChanged(command.workspaceId());
        publishSubscriptionAssigned(command.workspaceId(), subscription.getId(), pricingPlan.getId());
        auditSubscriptionAssigned(command.workspaceId(), subscription.getId(), pricingPlan.getId());
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

    private void publishSubscriptionAssigned(UUID workspaceId, UUID subscriptionId, UUID pricingPlanId) {
        if (domainEventPublisher == null) {
            return;
        }
        domainEventPublisher.publish(KafkaTopicConstants.WORKSPACE_SUBSCRIPTION_ASSIGNED, new BaseDomainEvent(
                KafkaTopicConstants.WORKSPACE_SUBSCRIPTION_ASSIGNED,
                workspaceId,
                subscriptionId,
                Instant.now(),
                Map.of(
                        "workspaceId", workspaceId.toString(),
                        "subscriptionId", subscriptionId.toString(),
                        "pricingPlanId", pricingPlanId.toString())));
    }

    private void auditSubscriptionAssigned(UUID workspaceId, UUID subscriptionId, UUID pricingPlanId) {
        if (auditLogService == null) {
            return;
        }
        auditLogService.appendCurrentUserAction(
                workspaceId,
                "workspace.subscription.assigned.%s".formatted(subscriptionId),
                AuditActionType.UPDATE,
                AuditOutcome.SUCCESS,
                "WorkspaceSubscription",
                subscriptionId,
                "Workspace subscription assigned",
                Map.of("workspaceId", workspaceId.toString(), "subscriptionId", subscriptionId.toString(), "pricingPlanId", pricingPlanId.toString()),
                null,
                null);
    }
}
