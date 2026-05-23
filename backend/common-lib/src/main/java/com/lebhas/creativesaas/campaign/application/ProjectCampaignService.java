package com.lebhas.creativesaas.campaign.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.campaign.application.dto.ProjectCampaignView;
import com.lebhas.creativesaas.campaign.domain.ProjectCampaignEntity;
import com.lebhas.creativesaas.campaign.domain.ProjectCampaignStatus;
import com.lebhas.creativesaas.campaign.infrastructure.persistence.ProjectCampaignRepository;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.identity.application.SessionProperties;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.project.infrastructure.persistence.ProjectRepository;
import com.lebhas.creativesaas.product.application.ProductServiceCatalogService;
import com.lebhas.creativesaas.product.domain.ProductServiceEntity;
import com.lebhas.creativesaas.redis.RedisCacheService;
import com.lebhas.creativesaas.redis.RedisKeyBuilder;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.workspace.application.WorkspaceActivityLogger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProjectCampaignService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final ProjectCampaignRepository projectCampaignRepository;
    private final ProjectCampaignViewMapper projectCampaignViewMapper;
    private final ProjectRepository projectRepository;
    private final ProductServiceCatalogService productServiceCatalogService;
    private final RedisCacheService redisCacheService;
    private final RedisKeyBuilder redisKeyBuilder;
    private final RedisLockService redisLockService;
    private final DomainEventPublisher domainEventPublisher;
    private final WorkspaceActivityLogger workspaceActivityLogger;
    private final SessionProperties sessionProperties;

    public ProjectCampaignService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            ProjectCampaignRepository projectCampaignRepository,
            ProjectCampaignViewMapper projectCampaignViewMapper,
            ProjectRepository projectRepository,
            ProductServiceCatalogService productServiceCatalogService,
            RedisCacheService redisCacheService,
            RedisKeyBuilder redisKeyBuilder,
            RedisLockService redisLockService,
            DomainEventPublisher domainEventPublisher,
            WorkspaceActivityLogger workspaceActivityLogger,
            SessionProperties sessionProperties
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.projectCampaignRepository = projectCampaignRepository;
        this.projectCampaignViewMapper = projectCampaignViewMapper;
        this.projectRepository = projectRepository;
        this.productServiceCatalogService = productServiceCatalogService;
        this.redisCacheService = redisCacheService;
        this.redisKeyBuilder = redisKeyBuilder;
        this.redisLockService = redisLockService;
        this.domainEventPublisher = domainEventPublisher;
        this.workspaceActivityLogger = workspaceActivityLogger;
        this.sessionProperties = sessionProperties;
    }

    @Transactional(readOnly = true)
    public List<ProjectCampaignView> listProjectCampaigns(UUID workspaceId) {
        workspaceAuthorizationService.requirePermission(workspaceId, Permission.PROJECT_VIEW);
        ProjectCampaignListCacheEntry cacheEntry = redisCacheService.getOrLoad(
                redisKeyBuilder.workspaceProjects(workspaceId),
                sessionProperties.getEntityCacheTtl(),
                ProjectCampaignListCacheEntry.class,
                () -> new ProjectCampaignListCacheEntry(
                        projectCampaignRepository.findAllByWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(workspaceId)
                                .stream()
                                .map(projectCampaignViewMapper::toView)
                                .toList(),
                        Instant.now()));
        List<ProjectCampaignView> campaignViews = cacheEntry.projects();
        if (!campaignViews.isEmpty()) {
            return campaignViews;
        }
        return projectRepository.findAllByWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(workspaceId).stream()
                .map(project -> new ProjectCampaignView(
                        project.getId(),
                        project.getWorkspaceId(),
                        project.getBrandId(),
                        null,
                        null,
                        project.getName(),
                        project.getDescription(),
                        project.getCampaignObjective() == null ? null : project.getCampaignObjective().name(),
                        project.getTargetPlatform() == null ? null : project.getTargetPlatform().name(),
                        "PROJECT",
                        project.getStatus() == null
                                ? com.lebhas.creativesaas.campaign.domain.ProjectCampaignStatus.ACTIVE
                                : com.lebhas.creativesaas.campaign.domain.ProjectCampaignStatus.valueOf(project.getStatus().name()),
                        project.getCreatedAt(),
                        project.getUpdatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectCampaignView getProjectCampaign(UUID workspaceId, UUID projectId) {
        workspaceAuthorizationService.requirePermission(workspaceId, Permission.PROJECT_VIEW);
        return redisCacheService.getOrLoad(
                redisKeyBuilder.project(projectId),
                sessionProperties.getEntityCacheTtl(),
                ProjectCampaignView.class,
                () -> projectCampaignViewMapper.toView(requireProjectCampaign(workspaceId, projectId)));
    }

    @Transactional
    public ProjectCampaignView createProjectCampaign(
            UUID workspaceId,
            UUID productServiceId,
            String name,
            String description,
            String campaignObjective,
            String targetPlatform,
            String campaignType
    ) {
        WorkspaceAuthorizationService.WorkspaceAccess access =
                workspaceAuthorizationService.requirePermission(workspaceId, Permission.PROJECT_CREATE);
        ProductServiceEntity productService = productServiceCatalogService.requireProductService(workspaceId, productServiceId);
        RedisLockService.RedisLockToken lockToken = acquireWorkspaceLock(workspaceId);
        try {
            ProjectCampaignEntity projectCampaign = projectCampaignRepository.save(ProjectCampaignEntity.create(
                    workspaceId,
                    productService.getBrandId(),
                    productService.getId(),
                    access.currentUser().userId(),
                    name,
                    description,
                    campaignObjective,
                    targetPlatform,
                    campaignType));
            ProjectCampaignView view = projectCampaignViewMapper.toView(projectCampaign);
            invalidateCaches(workspaceId, projectCampaign.getId(), access.currentUser().userId());
            workspaceActivityLogger.logProjectCampaignMutation("created", workspaceId, access.currentUser().userId(), projectCampaign.getId());
            publishSafely(
                    KafkaTopicConstants.PROJECT_CAMPAIGN_CREATED,
                    new BaseDomainEvent(
                            KafkaTopicConstants.PROJECT_CAMPAIGN_CREATED,
                            workspaceId,
                            projectCampaign.getId(),
                            Instant.now(),
                            Map.of(
                                    "projectId", projectCampaign.getId().toString(),
                                    "productServiceId", productServiceId.toString(),
                                    "brandId", productService.getBrandId().toString())));
            return view;
        } finally {
            redisLockService.release(lockToken);
        }
    }

    @Transactional
    public ProjectCampaignView updateProjectCampaign(
            UUID workspaceId,
            UUID projectId,
            String name,
            String description,
            String campaignObjective,
            String targetPlatform,
            String campaignType,
            ProjectCampaignStatus status
    ) {
        WorkspaceAuthorizationService.WorkspaceAccess access =
                workspaceAuthorizationService.requirePermission(workspaceId, Permission.PROJECT_UPDATE);
        RedisLockService.RedisLockToken lockToken = acquireProjectLock(projectId);
        try {
            ProjectCampaignEntity projectCampaign = requireProjectCampaign(workspaceId, projectId);
            projectCampaign.update(name, description, campaignObjective, targetPlatform, campaignType);
            if (status != null) {
                projectCampaign.changeStatus(status);
            }
            ProjectCampaignView view = projectCampaignViewMapper.toView(projectCampaignRepository.save(projectCampaign));
            invalidateCaches(workspaceId, projectId, access.currentUser().userId());
            workspaceActivityLogger.logProjectCampaignMutation("updated", workspaceId, access.currentUser().userId(), projectId);
            publishSafely(
                    KafkaTopicConstants.PROJECT_CAMPAIGN_UPDATED,
                    new BaseDomainEvent(
                            KafkaTopicConstants.PROJECT_CAMPAIGN_UPDATED,
                            workspaceId,
                            projectId,
                            Instant.now(),
                            Map.of("projectId", projectId.toString(), "productServiceId", projectCampaign.getProductServiceId().toString())));
            return view;
        } finally {
            redisLockService.release(lockToken);
        }
    }

    @Transactional
    public void deleteProjectCampaign(UUID workspaceId, UUID projectId) {
        WorkspaceAuthorizationService.WorkspaceAccess access =
                workspaceAuthorizationService.requirePermission(workspaceId, Permission.PROJECT_UPDATE);
        RedisLockService.RedisLockToken lockToken = acquireProjectLock(projectId);
        try {
            ProjectCampaignEntity projectCampaign = requireProjectCampaign(workspaceId, projectId);
            projectCampaign.changeStatus(ProjectCampaignStatus.ARCHIVED);
            projectCampaign.markDeleted();
            projectCampaignRepository.save(projectCampaign);
            invalidateCaches(workspaceId, projectId, access.currentUser().userId());
            workspaceActivityLogger.logProjectCampaignMutation("deleted", workspaceId, access.currentUser().userId(), projectId);
            publishSafely(
                    KafkaTopicConstants.PROJECT_CAMPAIGN_DELETED,
                    new BaseDomainEvent(
                            KafkaTopicConstants.PROJECT_CAMPAIGN_DELETED,
                            workspaceId,
                            projectId,
                            Instant.now(),
                            Map.of("projectId", projectId.toString())));
        } finally {
            redisLockService.release(lockToken);
        }
    }

    @Transactional(readOnly = true)
    public ProjectCampaignEntity requireProjectCampaign(UUID workspaceId, UUID projectId) {
        return projectCampaignRepository.findByIdAndWorkspaceIdAndDeletedFalse(projectId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_CAMPAIGN_NOT_FOUND));
    }

    private RedisLockService.RedisLockToken acquireWorkspaceLock(UUID workspaceId) {
        return redisLockService.acquire(redisKeyBuilder.lockWorkspace(workspaceId), Duration.ofSeconds(10))
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Workspace mutation is already in progress"));
    }

    private RedisLockService.RedisLockToken acquireProjectLock(UUID projectId) {
        return redisLockService.acquire(redisKeyBuilder.lockProject(projectId), Duration.ofSeconds(10))
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Project mutation is already in progress"));
    }

    private void invalidateCaches(UUID workspaceId, UUID projectId, UUID actorUserId) {
        redisCacheService.delete(redisKeyBuilder.workspaceProjects(workspaceId));
        redisCacheService.delete(redisKeyBuilder.project(projectId));
        workspaceActivityLogger.logCacheInvalidation(redisKeyBuilder.workspaceProjects(workspaceId), workspaceId, actorUserId);
        workspaceActivityLogger.logCacheInvalidation(redisKeyBuilder.project(projectId), workspaceId, actorUserId);
    }

    private void publishSafely(String topic, BaseDomainEvent event) {
        try {
            domainEventPublisher.publish(topic, event);
        } catch (RuntimeException ignored) {
        }
    }

    private record ProjectCampaignListCacheEntry(List<ProjectCampaignView> projects, Instant cachedAt) {
    }
}
