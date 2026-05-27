package com.lebhas.creativesaas.activity.application;

import com.lebhas.creativesaas.activity.cache.ActivityFeedCacheService;
import com.lebhas.creativesaas.activity.cache.WorkspaceTimelineCacheService;
import com.lebhas.creativesaas.activity.domain.ActivityCategory;
import com.lebhas.creativesaas.activity.domain.ActivityFeed;
import com.lebhas.creativesaas.activity.infrastructure.persistence.ActivityFeedRepository;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ActivityFeedService {

    public static final String TYPE_CREATIVE_REQUEST_CREATED = "CREATIVE_REQUEST_CREATED";
    public static final String TYPE_GENERATION_COMPLETED = "GENERATION_COMPLETED";
    public static final String TYPE_APPROVAL_ACTION = "APPROVAL_ACTION";
    public static final String TYPE_DOWNLOAD_COMPLETED = "DOWNLOAD_COMPLETED";
    public static final String TYPE_SHARE_CREATED = "SHARE_CREATED";
    public static final String TYPE_PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    public static final String TYPE_SUBSCRIPTION_CHANGED = "SUBSCRIPTION_CHANGED";
    public static final String TYPE_AI_PROVIDER_SWITCHED = "AI_PROVIDER_SWITCHED";
    public static final String TYPE_ROUTING_POLICY_CHANGED = "ROUTING_POLICY_CHANGED";
    public static final String TYPE_WORKSPACE_EVENT = "WORKSPACE_EVENT";
    public static final String TYPE_PROFILE_UPDATED = "PROFILE_UPDATED";
    public static final String TYPE_PROFILE_IMAGE_UPDATED = "PROFILE_IMAGE_UPDATED";
    public static final String TYPE_PASSWORD_CHANGED = "PASSWORD_CHANGED";

    private final ActivityFeedRepository activityFeedRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final ActivityFeedMapper activityFeedMapper;
    private ActivityFeedCacheService activityFeedCacheService;
    private WorkspaceTimelineCacheService workspaceTimelineCacheService;

    public ActivityFeedService(
            ActivityFeedRepository activityFeedRepository,
            WorkspaceAuthorizationService workspaceAuthorizationService,
            ActivityFeedMapper activityFeedMapper
    ) {
        this.activityFeedRepository = activityFeedRepository;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.activityFeedMapper = activityFeedMapper;
    }

    @Autowired(required = false)
    void setActivityFeedCacheService(ActivityFeedCacheService activityFeedCacheService) {
        this.activityFeedCacheService = activityFeedCacheService;
    }

    @Autowired(required = false)
    void setWorkspaceTimelineCacheService(WorkspaceTimelineCacheService workspaceTimelineCacheService) {
        this.workspaceTimelineCacheService = workspaceTimelineCacheService;
    }

    @Transactional
    public Optional<ActivityFeedView> create(ActivityFeedCommand command) {
        if (activityFeedRepository.existsBySourceEventIdAndDeletedFalse(command.sourceEventId())) {
            return Optional.empty();
        }
        ActivityFeed activityFeed = ActivityFeed.create(
                command.workspaceId(),
                command.sourceEventId(),
                command.actorUserId(),
                command.activityCategory(),
                command.activityType(),
                command.title(),
                command.description(),
                command.referenceType(),
                command.referenceId(),
                command.activityAt());
        ActivityFeed saved = activityFeedRepository.save(activityFeed);
        invalidateActivityCaches(saved.getWorkspaceId());
        return Optional.of(activityFeedMapper.toView(saved));
    }

    @Transactional
    public Optional<ActivityFeedView> creativeRequestCreated(
            UUID workspaceId,
            String sourceEventId,
            UUID actorUserId,
            UUID creativeRequestId,
            String title
    ) {
        return create(new ActivityFeedCommand(
                workspaceId,
                sourceEventId,
                actorUserId,
                ActivityCategory.CREATIVE_REQUEST,
                TYPE_CREATIVE_REQUEST_CREATED,
                titleOrDefault(title, "Creative request created"),
                null,
                "CREATIVE_REQUEST",
                creativeRequestId,
                Instant.now()));
    }

    @Transactional
    public Optional<ActivityFeedView> generationCompleted(
            UUID workspaceId,
            String sourceEventId,
            UUID actorUserId,
            UUID generatedVersionId,
            String title
    ) {
        return create(new ActivityFeedCommand(
                workspaceId,
                sourceEventId,
                actorUserId,
                ActivityCategory.GENERATED_VERSION,
                TYPE_GENERATION_COMPLETED,
                titleOrDefault(title, "Generation completed"),
                null,
                "GENERATED_VERSION",
                generatedVersionId,
                Instant.now()));
    }

    @Transactional
    public Optional<ActivityFeedView> approvalAction(
            UUID workspaceId,
            String sourceEventId,
            UUID actorUserId,
            UUID approvalId,
            String title,
            String description
    ) {
        return create(new ActivityFeedCommand(
                workspaceId,
                sourceEventId,
                actorUserId,
                ActivityCategory.APPROVAL,
                TYPE_APPROVAL_ACTION,
                titleOrDefault(title, "Approval action recorded"),
                description,
                "APPROVAL",
                approvalId,
                Instant.now()));
    }

    @Transactional
    public Optional<ActivityFeedView> downloadCompleted(
            UUID workspaceId,
            String sourceEventId,
            UUID actorUserId,
            UUID referenceId,
            String referenceType
    ) {
        return create(new ActivityFeedCommand(
                workspaceId,
                sourceEventId,
                actorUserId,
                ActivityCategory.ASSET,
                TYPE_DOWNLOAD_COMPLETED,
                "Download completed",
                null,
                referenceType,
                referenceId,
                Instant.now()));
    }

    @Transactional
    public Optional<ActivityFeedView> shareCreated(UUID workspaceId, String sourceEventId, UUID actorUserId, UUID shareLinkId) {
        return create(new ActivityFeedCommand(
                workspaceId,
                sourceEventId,
                actorUserId,
                ActivityCategory.SHARE,
                TYPE_SHARE_CREATED,
                "Share link created",
                null,
                "SHARE_LINK",
                shareLinkId,
                Instant.now()));
    }

    @Transactional
    public Optional<ActivityFeedView> paymentCompleted(UUID workspaceId, String sourceEventId, UUID actorUserId, UUID paymentReferenceId) {
        return create(new ActivityFeedCommand(
                workspaceId,
                sourceEventId,
                actorUserId,
                ActivityCategory.PAYMENT,
                TYPE_PAYMENT_COMPLETED,
                "Payment completed",
                null,
                "PAYMENT",
                paymentReferenceId,
                Instant.now()));
    }

    @Transactional
    public Optional<ActivityFeedView> subscriptionChanged(UUID workspaceId, String sourceEventId, UUID actorUserId, UUID subscriptionReferenceId) {
        return create(new ActivityFeedCommand(
                workspaceId,
                sourceEventId,
                actorUserId,
                ActivityCategory.PAYMENT,
                TYPE_SUBSCRIPTION_CHANGED,
                "Subscription changed",
                null,
                "SUBSCRIPTION",
                subscriptionReferenceId,
                Instant.now()));
    }

    @Transactional
    public Optional<ActivityFeedView> aiProviderSwitched(UUID workspaceId, String sourceEventId, UUID actorUserId, UUID providerReferenceId) {
        return create(new ActivityFeedCommand(
                workspaceId,
                sourceEventId,
                actorUserId,
                ActivityCategory.AI,
                TYPE_AI_PROVIDER_SWITCHED,
                "AI provider switched",
                null,
                "AI_PROVIDER",
                providerReferenceId,
                Instant.now()));
    }

    @Transactional
    public Optional<ActivityFeedView> routingPolicyChanged(UUID workspaceId, String sourceEventId, UUID actorUserId, UUID routingPolicyId) {
        return create(new ActivityFeedCommand(
                workspaceId,
                sourceEventId,
                actorUserId,
                ActivityCategory.AI,
                TYPE_ROUTING_POLICY_CHANGED,
                "Routing policy changed",
                null,
                "ROUTING_POLICY",
                routingPolicyId,
                Instant.now()));
    }

    @Transactional
    public Optional<ActivityFeedView> workspaceEvent(
            UUID workspaceId,
            String sourceEventId,
            UUID actorUserId,
            String title,
            String description
    ) {
        return create(new ActivityFeedCommand(
                workspaceId,
                sourceEventId,
                actorUserId,
                ActivityCategory.WORKSPACE,
                TYPE_WORKSPACE_EVENT,
                titleOrDefault(title, "Workspace event"),
                description,
                "WORKSPACE",
                workspaceId,
                Instant.now()));
    }

    @Transactional(readOnly = true)
    public List<ActivityFeedView> listWorkspaceActivities(UUID workspaceId, int limit) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceContext(workspaceId);
        Set<ActivityCategory> visibleCategories = visibleCategories(access);
        return activityFeedRepository
                .findAllByWorkspaceIdAndDeletedFalseOrderByActivityAtDesc(workspaceId, PageRequest.of(0, normalizeLimit(limit)))
                .stream()
                .filter(activityFeed -> visibleCategories.contains(activityFeed.getActivityCategory()))
                .map(activityFeedMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ActivityFeedView> listReferenceActivities(UUID workspaceId, String referenceType, UUID referenceId, int limit) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceContext(workspaceId);
        Set<ActivityCategory> visibleCategories = visibleCategories(access);
        return activityFeedRepository
                .findAllByWorkspaceIdAndReferenceTypeAndReferenceIdAndDeletedFalseOrderByActivityAtDesc(
                        workspaceId,
                        referenceType,
                        referenceId,
                        PageRequest.of(0, normalizeLimit(limit)))
                .stream()
                .filter(activityFeed -> visibleCategories.contains(activityFeed.getActivityCategory()))
                .map(activityFeedMapper::toView)
                .toList();
    }

    private static Set<ActivityCategory> visibleCategories(WorkspaceAuthorizationService.WorkspaceAccess access) {
        if (access.effectiveRole().isMaster() || access.effectiveRole() == com.lebhas.creativesaas.common.security.Role.ADMIN) {
            return Set.of(ActivityCategory.values());
        }
        Set<Permission> permissions = access.permissions() == null ? Set.of() : access.permissions();
        java.util.EnumSet<ActivityCategory> categories = java.util.EnumSet.noneOf(ActivityCategory.class);
        if (permissions.contains(Permission.WORKSPACE_VIEW)) {
            categories.add(ActivityCategory.WORKSPACE);
            categories.add(ActivityCategory.SYSTEM);
        }
        if (permissions.contains(Permission.CREATIVE_REQUEST_CREATE)
                || permissions.contains(Permission.CREATIVE_REQUEST_MANAGE)
                || permissions.contains(Permission.CREATIVE_GENERATE)) {
            categories.add(ActivityCategory.CREATIVE_REQUEST);
            categories.add(ActivityCategory.GENERATED_VERSION);
            categories.add(ActivityCategory.AI);
        }
        if (permissions.contains(Permission.CREATIVE_SUBMIT)
                || permissions.contains(Permission.GENERATED_VERSION_MANAGE)) {
            categories.add(ActivityCategory.APPROVAL);
        }
        if (permissions.contains(Permission.ASSET_VIEW)) {
            categories.add(ActivityCategory.ASSET);
            categories.add(ActivityCategory.SHARE);
        }
        return Set.copyOf(categories);
    }

    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 50;
        }
        return Math.min(limit, 200);
    }

    private static String titleOrDefault(String title, String defaultTitle) {
        return title == null || title.isBlank() ? defaultTitle : title.trim();
    }

    private void invalidateActivityCaches(UUID workspaceId) {
        if (activityFeedCacheService != null) {
            activityFeedCacheService.invalidateWorkspaceActivity(workspaceId);
        }
        if (workspaceTimelineCacheService != null) {
            workspaceTimelineCacheService.invalidateWorkspaceTimeline(workspaceId);
        }
    }
}
