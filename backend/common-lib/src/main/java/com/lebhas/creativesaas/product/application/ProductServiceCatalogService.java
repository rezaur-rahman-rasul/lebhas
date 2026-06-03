package com.lebhas.creativesaas.product.application;

import com.lebhas.creativesaas.brand.application.BrandService;
import com.lebhas.creativesaas.brand.domain.BrandEntity;
import com.lebhas.creativesaas.brand.domain.BrandStatus;
import com.lebhas.creativesaas.auditlog.application.AuditLogService;
import com.lebhas.creativesaas.auditlog.domain.AuditActionType;
import com.lebhas.creativesaas.auditlog.domain.AuditOutcome;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.identity.application.SessionProperties;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.product.domain.ProductServiceEntity;
import com.lebhas.creativesaas.product.domain.ProductServiceStatus;
import com.lebhas.creativesaas.product.application.dto.ProductServiceView;
import com.lebhas.creativesaas.product.infrastructure.persistence.ProductServiceRepository;
import com.lebhas.creativesaas.redis.RedisCacheService;
import com.lebhas.creativesaas.redis.RedisKeyBuilder;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.workspace.application.WorkspaceActivityLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductServiceCatalogService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final ProductServiceRepository productServiceRepository;
    private final ProductServiceViewMapper productServiceViewMapper;
    private final BrandService brandService;
    private final RedisCacheService redisCacheService;
    private final RedisKeyBuilder redisKeyBuilder;
    private final RedisLockService redisLockService;
    private final DomainEventPublisher domainEventPublisher;
    private final WorkspaceActivityLogger workspaceActivityLogger;
    private final SessionProperties sessionProperties;
    private AuditLogService auditLogService;

    public ProductServiceCatalogService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            ProductServiceRepository productServiceRepository,
            ProductServiceViewMapper productServiceViewMapper,
            BrandService brandService,
            RedisCacheService redisCacheService,
            RedisKeyBuilder redisKeyBuilder,
            RedisLockService redisLockService,
            DomainEventPublisher domainEventPublisher,
            WorkspaceActivityLogger workspaceActivityLogger,
            SessionProperties sessionProperties
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.productServiceRepository = productServiceRepository;
        this.productServiceViewMapper = productServiceViewMapper;
        this.brandService = brandService;
        this.redisCacheService = redisCacheService;
        this.redisKeyBuilder = redisKeyBuilder;
        this.redisLockService = redisLockService;
        this.domainEventPublisher = domainEventPublisher;
        this.workspaceActivityLogger = workspaceActivityLogger;
        this.sessionProperties = sessionProperties;
    }

    @Autowired(required = false)
    void setAuditLogService(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<ProductServiceView> listProductServices(UUID workspaceId) {
        workspaceAuthorizationService.requirePermission(workspaceId, Permission.PRODUCT_VIEW);
        ProductServiceListCacheEntry cacheEntry = redisCacheService.getOrLoad(
                redisKeyBuilder.workspaceProductServices(workspaceId),
                sessionProperties.getEntityCacheTtl(),
                ProductServiceListCacheEntry.class,
                () -> new ProductServiceListCacheEntry(
                        productServiceRepository.findAllByWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(workspaceId)
                                .stream()
                                .map(productServiceViewMapper::toView)
                                .toList(),
                        Instant.now()));
        return cacheEntry.productServices();
    }

    @Transactional(readOnly = true)
    public List<ProductServiceView> listProductServicesByBrand(UUID workspaceId, UUID brandId) {
        workspaceAuthorizationService.requirePermission(workspaceId, Permission.PRODUCT_VIEW);
        brandService.requireBrand(workspaceId, brandId);
        return listProductServices(workspaceId).stream()
                .filter(productService -> brandId.equals(productService.brandId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductServiceView getProductService(UUID workspaceId, UUID productServiceId) {
        workspaceAuthorizationService.requirePermission(workspaceId, Permission.PRODUCT_VIEW);
        return redisCacheService.getOrLoad(
                redisKeyBuilder.productService(productServiceId),
                sessionProperties.getEntityCacheTtl(),
                ProductServiceView.class,
                () -> productServiceViewMapper.toView(requireProductService(workspaceId, productServiceId)));
    }

    @Transactional
    public ProductServiceView createProductService(
            UUID workspaceId,
            UUID brandId,
            String name,
            String description,
            String category,
            String targetAudience,
            String sellingPoints
    ) {
        WorkspaceAuthorizationService.WorkspaceAccess access =
                workspaceAuthorizationService.requirePermission(workspaceId, Permission.PRODUCT_MANAGE);
        BrandEntity brand = brandService.requireBrand(workspaceId, brandId);
        if (brand.getStatus() != BrandStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Product/Service requires an active brand");
        }
        RedisLockService.RedisLockToken lockToken = acquireWorkspaceLock(workspaceId);
        try {
            ProductServiceEntity productService = productServiceRepository.save(ProductServiceEntity.create(
                    workspaceId,
                    brandId,
                    name,
                    description,
                    category,
                    targetAudience,
                    sellingPoints));
            ProductServiceView productServiceView = productServiceViewMapper.toView(productService);
            invalidateCaches(workspaceId, productService.getId(), access.currentUser().userId());
            workspaceActivityLogger.logProductServiceMutation("created", workspaceId, access.currentUser().userId(), productService.getId());
            auditMutation("created", AuditActionType.CREATE, workspaceId, access.currentUser().userId(), productService.getId(), productService.getName(), brandId);
            publishSafely(
                    KafkaTopicConstants.PRODUCT_SERVICE_CREATED,
                    new BaseDomainEvent(
                            KafkaTopicConstants.PRODUCT_SERVICE_CREATED,
                            workspaceId,
                            productService.getId(),
                            Instant.now(),
                            Map.of("productServiceId", productService.getId().toString(), "brandId", brandId.toString())));
            return productServiceView;
        } finally {
            redisLockService.release(lockToken);
        }
    }

    @Transactional
    public ProductServiceView updateProductService(
            UUID workspaceId,
            UUID productServiceId,
            String name,
            String description,
            String category,
            String targetAudience,
            String sellingPoints,
            ProductServiceStatus status
    ) {
        WorkspaceAuthorizationService.WorkspaceAccess access =
                workspaceAuthorizationService.requirePermission(workspaceId, Permission.PRODUCT_MANAGE);
        RedisLockService.RedisLockToken lockToken = acquireWorkspaceLock(workspaceId);
        try {
            ProductServiceEntity productService = requireProductService(workspaceId, productServiceId);
            productService.update(name, description, category, targetAudience, sellingPoints);
            if (status != null) {
                productService.changeStatus(status);
            }
            ProductServiceView productServiceView = productServiceViewMapper.toView(productServiceRepository.save(productService));
            invalidateCaches(workspaceId, productServiceId, access.currentUser().userId());
            workspaceActivityLogger.logProductServiceMutation("updated", workspaceId, access.currentUser().userId(), productServiceId);
            auditMutation("updated", AuditActionType.UPDATE, workspaceId, access.currentUser().userId(), productServiceId, productService.getName(), productService.getBrandId());
            publishSafely(
                    KafkaTopicConstants.PRODUCT_SERVICE_UPDATED,
                    new BaseDomainEvent(
                            KafkaTopicConstants.PRODUCT_SERVICE_UPDATED,
                            workspaceId,
                            productServiceId,
                            Instant.now(),
                            Map.of("productServiceId", productServiceId.toString(), "brandId", productService.getBrandId().toString())));
            return productServiceView;
        } finally {
            redisLockService.release(lockToken);
        }
    }

    @Transactional
    public void deleteProductService(UUID workspaceId, UUID productServiceId) {
        WorkspaceAuthorizationService.WorkspaceAccess access =
                workspaceAuthorizationService.requirePermission(workspaceId, Permission.PRODUCT_MANAGE);
        RedisLockService.RedisLockToken lockToken = acquireWorkspaceLock(workspaceId);
        try {
            ProductServiceEntity productService = requireProductService(workspaceId, productServiceId);
            productService.changeStatus(ProductServiceStatus.ARCHIVED);
            productService.markDeleted();
            productServiceRepository.save(productService);
            invalidateCaches(workspaceId, productServiceId, access.currentUser().userId());
            workspaceActivityLogger.logProductServiceMutation("deleted", workspaceId, access.currentUser().userId(), productServiceId);
            auditMutation("deleted", AuditActionType.DELETE, workspaceId, access.currentUser().userId(), productServiceId, productService.getName(), productService.getBrandId());
            publishSafely(
                    KafkaTopicConstants.PRODUCT_SERVICE_DELETED,
                    new BaseDomainEvent(
                            KafkaTopicConstants.PRODUCT_SERVICE_DELETED,
                            workspaceId,
                            productServiceId,
                            Instant.now(),
                            Map.of("productServiceId", productServiceId.toString())));
        } finally {
            redisLockService.release(lockToken);
        }
    }

    @Transactional(readOnly = true)
    public ProductServiceEntity requireProductService(UUID workspaceId, UUID productServiceId) {
        return productServiceRepository.findByIdAndWorkspaceIdAndDeletedFalse(productServiceId, workspaceId)
                .orElseThrow(() -> new com.lebhas.creativesaas.common.exception.BusinessException(com.lebhas.creativesaas.common.exception.ErrorCode.PRODUCT_SERVICE_NOT_FOUND));
    }

    private RedisLockService.RedisLockToken acquireWorkspaceLock(UUID workspaceId) {
        return redisLockService.acquire(redisKeyBuilder.lockWorkspace(workspaceId), Duration.ofSeconds(10))
                .orElseThrow(() -> new com.lebhas.creativesaas.common.exception.BusinessException(
                        com.lebhas.creativesaas.common.exception.ErrorCode.BUSINESS_RULE_VIOLATION,
                        "Workspace mutation is already in progress"));
    }

    private void invalidateCaches(UUID workspaceId, UUID productServiceId, UUID actorUserId) {
        redisCacheService.delete(redisKeyBuilder.workspaceProductServices(workspaceId));
        redisCacheService.delete(redisKeyBuilder.productService(productServiceId));
        workspaceActivityLogger.logCacheInvalidation(redisKeyBuilder.workspaceProductServices(workspaceId), workspaceId, actorUserId);
        workspaceActivityLogger.logCacheInvalidation(redisKeyBuilder.productService(productServiceId), workspaceId, actorUserId);
    }

    private void publishSafely(String topic, BaseDomainEvent event) {
        try {
            domainEventPublisher.publish(topic, event);
        } catch (RuntimeException ignored) {
        }
    }

    private void auditMutation(
            String action,
            AuditActionType auditAction,
            UUID workspaceId,
            UUID actorUserId,
            UUID productServiceId,
            String name,
            UUID brandId
    ) {
        if (auditLogService == null) {
            return;
        }
        auditLogService.appendUserAction(
                workspaceId,
                "product_service.%s.%s".formatted(action, productServiceId),
                actorUserId,
                auditAction,
                AuditOutcome.SUCCESS,
                "ProductService",
                productServiceId,
                "Product/Service %s".formatted(action),
                Map.of(
                        "productServiceId", productServiceId.toString(),
                        "brandId", brandId.toString(),
                        "name", name == null ? "" : name),
                null,
                null);
    }

    private record ProductServiceListCacheEntry(List<ProductServiceView> productServices, Instant cachedAt) {
    }
}
