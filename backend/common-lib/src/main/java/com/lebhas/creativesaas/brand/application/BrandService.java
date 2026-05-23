package com.lebhas.creativesaas.brand.application;

import com.lebhas.creativesaas.brand.application.dto.BrandView;
import com.lebhas.creativesaas.brand.domain.BrandEntity;
import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.brand.domain.BrandStatus;
import com.lebhas.creativesaas.brand.infrastructure.persistence.BrandRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.identity.application.SessionProperties;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.redis.RedisCacheService;
import com.lebhas.creativesaas.redis.RedisKeyBuilder;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.workspace.application.WorkspaceActivityLogger;
import com.lebhas.creativesaas.workspace.domain.WorkspaceEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BrandService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final BrandRepository brandRepository;
    private final BrandViewMapper brandViewMapper;
    private final RedisCacheService redisCacheService;
    private final RedisKeyBuilder redisKeyBuilder;
    private final RedisLockService redisLockService;
    private final DomainEventPublisher domainEventPublisher;
    private final WorkspaceActivityLogger workspaceActivityLogger;
    private final SessionProperties sessionProperties;

    public BrandService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            BrandRepository brandRepository,
            BrandViewMapper brandViewMapper,
            RedisCacheService redisCacheService,
            RedisKeyBuilder redisKeyBuilder,
            RedisLockService redisLockService,
            DomainEventPublisher domainEventPublisher,
            WorkspaceActivityLogger workspaceActivityLogger,
            SessionProperties sessionProperties
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.brandRepository = brandRepository;
        this.brandViewMapper = brandViewMapper;
        this.redisCacheService = redisCacheService;
        this.redisKeyBuilder = redisKeyBuilder;
        this.redisLockService = redisLockService;
        this.domainEventPublisher = domainEventPublisher;
        this.workspaceActivityLogger = workspaceActivityLogger;
        this.sessionProperties = sessionProperties;
    }

    @Transactional(readOnly = true)
    public List<BrandView> listBrands(UUID workspaceId) {
        workspaceAuthorizationService.requirePermission(workspaceId, Permission.BRAND_VIEW);
        BrandListCacheEntry cacheEntry = redisCacheService.getOrLoad(
                redisKeyBuilder.workspaceBrands(workspaceId),
                sessionProperties.getEntityCacheTtl(),
                BrandListCacheEntry.class,
                () -> new BrandListCacheEntry(
                        brandRepository.findAllByWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(workspaceId).stream()
                                .map(brandViewMapper::toView)
                                .toList(),
                        Instant.now()));
        return cacheEntry.brands();
    }

    @Transactional(readOnly = true)
    public BrandView getBrand(UUID workspaceId, UUID brandId) {
        workspaceAuthorizationService.requirePermission(workspaceId, Permission.BRAND_VIEW);
        return redisCacheService.getOrLoad(
                redisKeyBuilder.brand(brandId),
                sessionProperties.getEntityCacheTtl(),
                BrandView.class,
                () -> brandViewMapper.toView(requireBrand(workspaceId, brandId)));
    }

    @Transactional
    public BrandView createBrand(
            UUID workspaceId,
            String name,
            String businessType,
            String industry,
            String targetAudience,
            String brandVoice,
            String preferredCta,
            String primaryColor,
            String secondaryColor,
            String website,
            String facebookUrl,
            String instagramUrl,
            String linkedinUrl,
            String tiktokUrl,
            BrandLanguagePreference languagePreference
    ) {
        WorkspaceAuthorizationService.WorkspaceAccess access =
                workspaceAuthorizationService.requirePermission(workspaceId, Permission.BRAND_MANAGE);
        RedisLockService.RedisLockToken lockToken = acquireWorkspaceLock(workspaceId);
        try {
            BrandEntity brand = brandRepository.save(BrandEntity.create(
                    workspaceId,
                    access.currentUser().userId(),
                    name,
                    businessType,
                    industry,
                    targetAudience,
                    brandVoice,
                    preferredCta,
                    primaryColor,
                    secondaryColor,
                    website,
                    facebookUrl,
                    instagramUrl,
                    linkedinUrl,
                    tiktokUrl,
                    languagePreference));
            BrandView brandView = brandViewMapper.toView(brand);
            invalidateBrandCaches(workspaceId, brand.getId(), access.currentUser().userId());
            workspaceActivityLogger.logBrandMutation("created", workspaceId, access.currentUser().userId(), brand.getId());
            publishSafely(
                    KafkaTopicConstants.BRAND_CREATED,
                    new BaseDomainEvent(
                            KafkaTopicConstants.BRAND_CREATED,
                            workspaceId,
                            brand.getId(),
                            Instant.now(),
                            Map.of("brandId", brand.getId().toString(), "name", brand.getName())));
            return brandView;
        } finally {
            redisLockService.release(lockToken);
        }
    }

    @Transactional
    public BrandView updateBrand(
            UUID workspaceId,
            UUID brandId,
            String name,
            String businessType,
            String industry,
            String targetAudience,
            String brandVoice,
            String preferredCta,
            String primaryColor,
            String secondaryColor,
            String website,
            String facebookUrl,
            String instagramUrl,
            String linkedinUrl,
            String tiktokUrl,
            BrandLanguagePreference languagePreference,
            BrandStatus status
    ) {
        WorkspaceAuthorizationService.WorkspaceAccess access =
                workspaceAuthorizationService.requirePermission(workspaceId, Permission.BRAND_MANAGE);
        RedisLockService.RedisLockToken lockToken = acquireWorkspaceLock(workspaceId);
        try {
            BrandEntity brand = requireBrand(workspaceId, brandId);
            brand.update(
                    name,
                    businessType,
                    industry,
                    targetAudience,
                    brandVoice,
                    preferredCta,
                    primaryColor,
                    secondaryColor,
                    website,
                    facebookUrl,
                    instagramUrl,
                    linkedinUrl,
                    tiktokUrl,
                    languagePreference);
            if (status != null) {
                brand.changeStatus(status);
            }
            BrandView brandView = brandViewMapper.toView(brandRepository.save(brand));
            invalidateBrandCaches(workspaceId, brandId, access.currentUser().userId());
            workspaceActivityLogger.logBrandMutation("updated", workspaceId, access.currentUser().userId(), brandId);
            publishSafely(
                    KafkaTopicConstants.BRAND_UPDATED,
                    new BaseDomainEvent(
                            KafkaTopicConstants.BRAND_UPDATED,
                            workspaceId,
                            brandId,
                            Instant.now(),
                            Map.of("brandId", brandId.toString(), "name", brand.getName())));
            return brandView;
        } finally {
            redisLockService.release(lockToken);
        }
    }

    @Transactional
    public void deleteBrand(UUID workspaceId, UUID brandId) {
        WorkspaceAuthorizationService.WorkspaceAccess access =
                workspaceAuthorizationService.requirePermission(workspaceId, Permission.BRAND_MANAGE);
        RedisLockService.RedisLockToken lockToken = acquireWorkspaceLock(workspaceId);
        try {
            BrandEntity brand = requireBrand(workspaceId, brandId);
            brand.changeStatus(BrandStatus.ARCHIVED);
            brand.markDeleted();
            brandRepository.save(brand);
            invalidateBrandCaches(workspaceId, brandId, access.currentUser().userId());
            workspaceActivityLogger.logBrandMutation("deleted", workspaceId, access.currentUser().userId(), brandId);
            publishSafely(
                    KafkaTopicConstants.BRAND_DELETED,
                    new BaseDomainEvent(
                            KafkaTopicConstants.BRAND_DELETED,
                            workspaceId,
                            brandId,
                            Instant.now(),
                            Map.of("brandId", brandId.toString())));
        } finally {
            redisLockService.release(lockToken);
        }
    }

    @Transactional(readOnly = true)
    public BrandEntity requirePrimaryBrand(UUID workspaceId) {
        return brandRepository.findFirstByWorkspaceIdAndDeletedFalseOrderByCreatedAtAsc(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BRAND_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public BrandEntity requireBrand(UUID workspaceId, UUID brandId) {
        return brandRepository.findByIdAndWorkspaceIdAndDeletedFalse(brandId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BRAND_NOT_FOUND));
    }

    @Transactional
    public BrandEntity ensurePrimaryBrand(WorkspaceEntity workspace) {
        return brandRepository.findFirstByWorkspaceIdAndDeletedFalseOrderByCreatedAtAsc(workspace.getId())
                .orElseGet(() -> brandRepository.save(BrandEntity.create(
                        workspace.getId(),
                        workspace.getOwnerId(),
                        workspace.getName(),
                        null,
                        workspace.getIndustry(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)));
    }

    @Transactional
    public BrandEntity syncPrimaryBrand(
            WorkspaceEntity workspace,
            String name,
            String businessType,
            String industry,
            String targetAudience,
            String brandVoice,
            String preferredCta,
            String primaryColor,
            String secondaryColor,
            String website,
            String facebookUrl,
            String instagramUrl,
            String linkedinUrl,
            String tiktokUrl
    ) {
        BrandEntity brand = ensurePrimaryBrand(workspace);
        brand.update(
                name,
                businessType,
                industry,
                targetAudience,
                brandVoice,
                preferredCta,
                primaryColor,
                secondaryColor,
                website,
                facebookUrl,
                instagramUrl,
                linkedinUrl,
                tiktokUrl,
                null);
        return brandRepository.save(brand);
    }

    private RedisLockService.RedisLockToken acquireWorkspaceLock(UUID workspaceId) {
        return redisLockService.acquire(redisKeyBuilder.lockWorkspace(workspaceId), Duration.ofSeconds(10))
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Workspace mutation is already in progress"));
    }

    private void invalidateBrandCaches(UUID workspaceId, UUID brandId, UUID actorUserId) {
        redisCacheService.delete(redisKeyBuilder.workspaceBrands(workspaceId));
        redisCacheService.delete(redisKeyBuilder.brand(brandId));
        workspaceActivityLogger.logCacheInvalidation(redisKeyBuilder.workspaceBrands(workspaceId), workspaceId, actorUserId);
        workspaceActivityLogger.logCacheInvalidation(redisKeyBuilder.brand(brandId), workspaceId, actorUserId);
    }

    private void publishSafely(String topic, BaseDomainEvent event) {
        try {
            domainEventPublisher.publish(topic, event);
        } catch (RuntimeException ignored) {
        }
    }

    private record BrandListCacheEntry(List<BrandView> brands, Instant cachedAt) {
    }
}
