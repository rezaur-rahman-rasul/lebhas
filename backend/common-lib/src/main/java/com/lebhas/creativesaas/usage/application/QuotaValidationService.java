package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import com.lebhas.creativesaas.redis.RedisKeyBuilder;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.pricing.PlanFeaturePolicy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class QuotaValidationService {

    private static final Duration QUOTA_LOCK_TTL = Duration.ofSeconds(5);

    private final PlanUsagePolicyResolver planUsagePolicyResolver;
    private final WorkspaceLimitService workspaceLimitService;
    private final ObjectProvider<RedisLockService> redisLockServiceProvider;
    private final RedisKeyBuilder redisKeyBuilder;

    public QuotaValidationService(
            PlanUsagePolicyResolver planUsagePolicyResolver,
            WorkspaceLimitService workspaceLimitService,
            ObjectProvider<RedisLockService> redisLockServiceProvider,
            RedisKeyBuilder redisKeyBuilder
    ) {
        this.planUsagePolicyResolver = planUsagePolicyResolver;
        this.workspaceLimitService = workspaceLimitService;
        this.redisLockServiceProvider = redisLockServiceProvider;
        this.redisKeyBuilder = redisKeyBuilder;
    }

    @Transactional(readOnly = true)
    public void validateGeneratedVersionsPerRequest(UUID workspaceId, UUID creativeRequestId, int requestedAdditionalVersions) {
        withOptionalQuotaLock(workspaceId, () -> {
            PlanFeaturePolicy policy = policy(workspaceId);
            Integer limit = policy.getMaxGeneratedVersionsPerRequest();
            if (limit == null) {
                return null;
            }
            long existing = workspaceLimitService.generatedVersionsForRequest(workspaceId, creativeRequestId);
            long requestedTotal = existing + Math.max(requestedAdditionalVersions, 0);
            if (requestedTotal > limit) {
                throw quotaExceeded("Generated version limit exceeded");
            }
            return null;
        });
    }

    @Transactional(readOnly = true)
    public void validateMonthlyCreditLimit(UUID workspaceId, BigDecimal creditsToReserve) {
        withOptionalQuotaLock(workspaceId, () -> {
            PlanFeaturePolicy policy = policy(workspaceId);
            BigDecimal limit = policy.getMonthlyCreditLimit();
            if (limit == null) {
                return null;
            }
            BigDecimal projected = workspaceLimitService.usedCreditsThisMonth(workspaceId)
                    .add(workspaceLimitService.reservedCreditsThisMonth(workspaceId))
                    .add(normalize(creditsToReserve));
            if (projected.compareTo(limit) > 0) {
                throw quotaExceeded("Monthly credit limit exceeded");
            }
            return null;
        });
    }

    @Transactional(readOnly = true)
    public void validateStorageLimit(UUID workspaceId, long additionalBytes) {
        withOptionalQuotaLock(workspaceId, () -> {
            PlanFeaturePolicy policy = policy(workspaceId);
            BigDecimal limit = policy.getMaxStorageGb();
            if (limit == null) {
                return null;
            }
            BigDecimal additionalGb = BigDecimal.valueOf(Math.max(additionalBytes, 0L))
                    .divide(new BigDecimal("1073741824"), 4, java.math.RoundingMode.HALF_UP);
            BigDecimal projected = workspaceLimitService.storageUsedGb(workspaceId).add(additionalGb);
            if (projected.compareTo(limit) > 0) {
                throw quotaExceeded("Storage limit exceeded");
            }
            return null;
        });
    }

    @Transactional(readOnly = true)
    public void validatePublicShareLinksAllowed(UUID workspaceId) {
        if (!policy(workspaceId).isAllowPublicShareLinks()) {
            throw featureDisabled("Public share links are not enabled for the active plan");
        }
    }

    @Transactional(readOnly = true)
    public void validateApprovalWorkflowAllowed(UUID workspaceId) {
        if (!policy(workspaceId).isAllowApprovalWorkflow()) {
            throw featureDisabled("Approval workflow is not enabled for the active plan");
        }
    }

    @Transactional(readOnly = true)
    public void validateVideoGenerationAllowed(UUID workspaceId, CreativeType creativeType) {
        if (creativeType != null && creativeType.isVideo() && !policy(workspaceId).isAllowVideoGeneration()) {
            throw featureDisabled("Video generation is not enabled for the active plan");
        }
    }

    @Transactional(readOnly = true)
    public void validateTeamMemberLimit(UUID workspaceId, int additionalMembers) {
        withOptionalQuotaLock(workspaceId, () -> {
            PlanFeaturePolicy policy = policy(workspaceId);
            Integer limit = policy.getMaxTeamMembers();
            if (limit == null) {
                return null;
            }
            long projected = workspaceLimitService.activeTeamMembers(workspaceId) + Math.max(additionalMembers, 0);
            if (projected > limit) {
                throw quotaExceeded("Team member limit exceeded");
            }
            return null;
        });
    }

    private PlanFeaturePolicy policy(UUID workspaceId) {
        return planUsagePolicyResolver.resolve(workspaceId).featurePolicy();
    }

    private <T> T withOptionalQuotaLock(UUID workspaceId, Supplier<T> action) {
        RedisLockService redisLockService = redisLockServiceProvider.getIfAvailable();
        if (redisLockService == null) {
            return action.get();
        }
        return redisLockService.acquire(redisKeyBuilder.uploadQuotaLock(workspaceId), QUOTA_LOCK_TTL)
                .map(token -> {
                    try {
                        return action.get();
                    } finally {
                        redisLockService.releaseQuietly(token);
                    }
                })
                .orElseGet(action);
    }

    private BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        return amount;
    }

    private BusinessException quotaExceeded(String message) {
        return new BusinessException(ErrorCode.PLAN_QUOTA_EXCEEDED, message);
    }

    private BusinessException featureDisabled(String message) {
        return new BusinessException(ErrorCode.PLAN_FEATURE_DISABLED, message);
    }
}
