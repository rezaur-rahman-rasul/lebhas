package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.profile.application.dto.SecurityActivityView;
import com.lebhas.creativesaas.profile.cache.UserSecurityActivityCacheService;
import com.lebhas.creativesaas.profile.domain.UserSecurityActivity;
import com.lebhas.creativesaas.profile.domain.UserSecurityActivityType;
import com.lebhas.creativesaas.profile.event.ProfileEventProducer;
import com.lebhas.creativesaas.profile.infrastructure.persistence.UserSecurityActivityRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserSecurityActivityService {

    private static final int DEFAULT_RECENT_LIMIT = 20;

    private final CurrentUserContext currentUserContext;
    private final UserSecurityActivityRepository userSecurityActivityRepository;
    private final UserSecurityActivityMapper userSecurityActivityMapper;
    private final UserSecurityActivityCacheService userSecurityActivityCacheService;
    private final ProfileEventProducer profileEventProducer;

    public UserSecurityActivityService(
            CurrentUserContext currentUserContext,
            UserSecurityActivityRepository userSecurityActivityRepository,
            UserSecurityActivityMapper userSecurityActivityMapper,
            UserSecurityActivityCacheService userSecurityActivityCacheService,
            ProfileEventProducer profileEventProducer
    ) {
        this.currentUserContext = currentUserContext;
        this.userSecurityActivityRepository = userSecurityActivityRepository;
        this.userSecurityActivityMapper = userSecurityActivityMapper;
        this.userSecurityActivityCacheService = userSecurityActivityCacheService;
        this.profileEventProducer = profileEventProducer;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SecurityActivityView record(
            UUID userId,
            UserSecurityActivityType activityType,
            String ipAddress,
            String userAgent,
            String locationHint,
            boolean success,
            String failureReason
    ) {
        UserSecurityActivity saved = userSecurityActivityRepository.save(UserSecurityActivity.record(
                userId,
                activityType,
                ipAddress,
                userAgent,
                locationHint,
                success,
                failureReason));
        userSecurityActivityCacheService.invalidateRecent(userId);
        UUID actorUserId = currentUserContext.getCurrentUser().map(com.lebhas.creativesaas.common.security.context.CurrentUser::userId).orElse(userId);
        UUID workspaceId = currentUserContext.getCurrentUser().map(com.lebhas.creativesaas.common.security.context.CurrentUser::workspaceId).orElse(null);
        profileEventProducer.profileSecurityActivityCreated(
                workspaceId,
                saved.getId(),
                userId,
                actorUserId,
                saved.getActivityType(),
                saved.isSuccess(),
                saved.getFailureReason());
        return userSecurityActivityMapper.toView(saved);
    }

    @Transactional(readOnly = true)
    public List<SecurityActivityView> listOwnRecentSecurityActivities(int limit) {
        UUID userId = currentUserContext.requireCurrentUser().userId();
        return userSecurityActivityCacheService.getRecent(userId)
                .map(UserSecurityActivityCacheService.RecentSecurityActivityCacheEntry::activities)
                .orElseGet(() -> loadAndCacheRecent(userId, limit));
    }

    @Transactional(readOnly = true)
    public List<SecurityActivityView> listRecentSecurityActivities(UUID userId, int limit) {
        return userSecurityActivityRepository
                .findAllByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId, PageRequest.of(0, normalizeLimit(limit)))
                .stream()
                .map(userSecurityActivityMapper::toView)
                .toList();
    }

    private List<SecurityActivityView> loadAndCacheRecent(UUID userId, int limit) {
        List<SecurityActivityView> activities = listRecentSecurityActivities(userId, limit);
        userSecurityActivityCacheService.cacheRecent(userId, activities);
        return activities;
    }

    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_RECENT_LIMIT;
        }
        return Math.min(limit, 100);
    }
}
