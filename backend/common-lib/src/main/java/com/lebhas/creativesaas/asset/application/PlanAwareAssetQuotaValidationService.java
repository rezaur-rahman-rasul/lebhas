package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.asset.domain.AssetType;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.pricing.application.WorkspacePlanContextService;
import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.PricingPlanView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspacePlanContextView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspaceSubscriptionView;
import com.lebhas.creativesaas.redis.RedisKeyBuilder;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.storage.application.StorageUsageService;
import com.lebhas.creativesaas.storage.domain.StorageUsageEntity;
import com.lebhas.creativesaas.storage.event.AssetUploadBlockedByPlanEvent;
import com.lebhas.creativesaas.storage.event.StorageLimitExceededEvent;
import com.lebhas.creativesaas.storage.producer.StoragePlanEventProducer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class PlanAwareAssetQuotaValidationService {

    private static final Duration QUOTA_LOCK_TTL = Duration.ofMinutes(5);
    private static final BigDecimal BYTES_PER_GB = BigDecimal.valueOf(1024L * 1024L * 1024L);

    private final WorkspacePlanContextService workspacePlanContextService;
    private final StorageUsageService storageUsageService;
    private final RedisLockService redisLockService;
    private final RedisKeyBuilder redisKeyBuilder;
    private final AssetServiceProperties assetServiceProperties;
    private final StoragePlanEventProducer storagePlanEventProducer;
    private final AssetActivityLogger assetActivityLogger;

    public PlanAwareAssetQuotaValidationService(
            WorkspacePlanContextService workspacePlanContextService,
            StorageUsageService storageUsageService,
            RedisLockService redisLockService,
            RedisKeyBuilder redisKeyBuilder,
            AssetServiceProperties assetServiceProperties,
            StoragePlanEventProducer storagePlanEventProducer,
            AssetActivityLogger assetActivityLogger
    ) {
        this.workspacePlanContextService = workspacePlanContextService;
        this.storageUsageService = storageUsageService;
        this.redisLockService = redisLockService;
        this.redisKeyBuilder = redisKeyBuilder;
        this.assetServiceProperties = assetServiceProperties;
        this.storagePlanEventProducer = storagePlanEventProducer;
        this.assetActivityLogger = assetActivityLogger;
    }

    @Transactional(readOnly = true)
    public PlanUploadPolicy resolveUploadPolicy(UUID workspaceId, AssetType assetType) {
        WorkspacePlanContextView planContext = workspacePlanContextService.getWorkspacePlanContext(workspaceId);
        PlanFeaturePolicyView featurePolicy = planContext.featurePolicy();
        Long storageLimitBytes = resolveStorageLimitBytes(featurePolicy);
        long allowedUploadSizeBytes = resolveAllowedUploadSizeBytes(assetType);
        return new PlanUploadPolicy(
                planContext.subscription(),
                planContext.pricingPlan(),
                featurePolicy,
                storageLimitBytes,
                allowedUploadSizeBytes,
                featurePolicy != null && featurePolicy.allowPublicShareLinks(),
                featurePolicy != null && featurePolicy.allowExportWithoutWatermark());
    }

    @Transactional(readOnly = true)
    public boolean isPublicShareAvailable(UUID workspaceId) {
        return resolveUploadPolicy(workspaceId, AssetType.RAW_IMAGE).publicShareAvailable();
    }

    @Transactional(readOnly = true)
    public boolean isExportWithoutWatermarkAvailable(UUID workspaceId) {
        return resolveUploadPolicy(workspaceId, AssetType.EXPORT_IMAGE).exportWithoutWatermarkAvailable();
    }

    @Transactional(readOnly = true)
    public long resolveAllowedUploadSizeBytes(UUID workspaceId, AssetType assetType) {
        return resolveUploadPolicy(workspaceId, assetType).allowedUploadSizeBytes();
    }

    @Transactional
    public UploadQuotaGuard acquireUploadQuotaGuard(
            UUID workspaceId,
            UUID assetId,
            UUID projectId,
            AssetType assetType,
            long incomingBytes
    ) {
        PlanUploadPolicy policy = resolveUploadPolicy(workspaceId, assetType);
        RedisLockService.RedisLockToken lockToken = redisLockService.acquire(
                        redisKeyBuilder.uploadQuotaLock(workspaceId),
                        QUOTA_LOCK_TTL)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.BUSINESS_RULE_VIOLATION,
                        "Asset upload quota validation is already in progress"));

        try {
            StorageUsageEntity usage = storageUsageService.getAuthoritativeSnapshot(workspaceId);
            long normalizedIncomingBytes = Math.max(incomingBytes, 0L);
            long projectedUsageBytes = usage.getTotalUsedBytes() + normalizedIncomingBytes;
            Long storageLimitBytes = policy.maxStorageBytes();

            if (storageLimitBytes != null && projectedUsageBytes > storageLimitBytes) {
                publishLimitExceededEvents(
                        workspaceId,
                        assetId,
                        projectId,
                        assetType,
                        usage,
                        normalizedIncomingBytes,
                        projectedUsageBytes,
                        policy);
                assetActivityLogger.logValidationFailure(workspaceId, null, "storage_limit_exceeded_for_pricing_plan");
                throw new BusinessException(
                        ErrorCode.BUSINESS_RULE_VIOLATION,
                        "Storage limit exceeded for current pricing plan",
                        java.util.List.of());
            }

            return new UploadQuotaGuard(
                    lockToken,
                    policy,
                    usage,
                    normalizedIncomingBytes,
                    projectedUsageBytes,
                    Instant.now());
        } catch (RuntimeException exception) {
            redisLockService.releaseQuietly(lockToken);
            throw exception;
        }
    }

    public void releaseUploadQuotaGuard(UploadQuotaGuard guard) {
        if (guard == null) {
            return;
        }
        redisLockService.releaseQuietly(guard.lockToken());
    }

    private Long resolveStorageLimitBytes(PlanFeaturePolicyView featurePolicy) {
        if (featurePolicy != null && featurePolicy.maxStorageGb() != null) {
            BigDecimal bytes = featurePolicy.maxStorageGb().multiply(BYTES_PER_GB);
            if (bytes.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0) {
                return Long.MAX_VALUE;
            }
            return bytes.setScale(0, RoundingMode.DOWN).longValue();
        }
        return assetServiceProperties.isWorkspaceStorageLimited()
                ? Math.max(assetServiceProperties.getMaxWorkspaceStorageBytes(), 0L)
                : null;
    }

    private long resolveAllowedUploadSizeBytes(AssetType assetType) {
        if (assetType == AssetType.BRAND_LOGO) {
            return Math.max(assetServiceProperties.getMaxLogoSizeBytes(), 0L);
        }
        if (assetType == AssetType.EXPORT_VIDEO) {
            return Math.max(assetServiceProperties.getMaxVideoSizeBytes(), 0L);
        }
        if (assetType == AssetType.RAW_IMAGE
                || assetType == AssetType.PRODUCT_IMAGE
                || assetType == AssetType.PACKAGING_IMAGE
                || assetType == AssetType.EXPORT_IMAGE
                || assetType == AssetType.THUMBNAIL) {
            return Math.max(assetServiceProperties.getMaxImageSizeBytes(), 0L);
        }
        return Math.max(assetServiceProperties.getMaxUploadSizeBytes(), 0L);
    }

    private void publishLimitExceededEvents(
            UUID workspaceId,
            UUID assetId,
            UUID projectId,
            AssetType assetType,
            StorageUsageEntity usage,
            long incomingBytes,
            long projectedUsageBytes,
            PlanUploadPolicy policy
    ) {
        try {
            storagePlanEventProducer.publishStorageLimitExceeded(new StorageLimitExceededEvent(
                    null,
                    Instant.now(),
                    workspaceId,
                    assetId,
                    projectId,
                    policy.pricingPlan() == null ? null : policy.pricingPlan().id(),
                    policy.subscription() == null ? null : policy.subscription().id(),
                    policy.pricingPlan() == null ? null : policy.pricingPlan().code(),
                    policy.subscription() == null || policy.subscription().status() == null ? null : policy.subscription().status().name(),
                    assetType == null ? null : assetType.name(),
                    usage.getTotalUsedBytes(),
                    incomingBytes,
                    projectedUsageBytes,
                    policy.maxStorageBytes(),
                    policy.allowedUploadSizeBytes(),
                    "STORAGE_LIMIT_EXCEEDED"));
            storagePlanEventProducer.publishAssetUploadBlockedByPlan(new AssetUploadBlockedByPlanEvent(
                    null,
                    Instant.now(),
                    workspaceId,
                    assetId,
                    projectId,
                    policy.pricingPlan() == null ? null : policy.pricingPlan().id(),
                    policy.subscription() == null ? null : policy.subscription().id(),
                    policy.pricingPlan() == null ? null : policy.pricingPlan().code(),
                    policy.subscription() == null || policy.subscription().status() == null ? null : policy.subscription().status().name(),
                    assetType == null ? null : assetType.name(),
                    usage.getTotalUsedBytes(),
                    incomingBytes,
                    projectedUsageBytes,
                    policy.maxStorageBytes(),
                    policy.allowedUploadSizeBytes(),
                    "ASSET_UPLOAD_BLOCKED_BY_PLAN"));
        } catch (RuntimeException exception) {
            assetActivityLogger.logKafkaFailure("storage-plan-quota", workspaceId, assetId, exception.getMessage());
        }
    }

    public record UploadQuotaGuard(
            RedisLockService.RedisLockToken lockToken,
            PlanUploadPolicy policy,
            StorageUsageEntity usage,
            long incomingBytes,
            long projectedUsageBytes,
            Instant checkedAt
    ) {
    }

    public record PlanUploadPolicy(
            WorkspaceSubscriptionView subscription,
            PricingPlanView pricingPlan,
            PlanFeaturePolicyView featurePolicy,
            Long maxStorageBytes,
            Long allowedUploadSizeBytes,
            boolean publicShareAvailable,
            boolean exportWithoutWatermarkAvailable
    ) {
    }
}
