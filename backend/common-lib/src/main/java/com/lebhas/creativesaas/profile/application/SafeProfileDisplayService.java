package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipStatus;
import com.lebhas.creativesaas.identity.infrastructure.persistence.WorkspaceMembershipRepository;
import com.lebhas.creativesaas.profile.application.dto.SafeProfileDisplayView;
import com.lebhas.creativesaas.profile.application.dto.UserProfileView;
import com.lebhas.creativesaas.profile.cache.ProfileImageUrlCacheService;
import com.lebhas.creativesaas.profile.cache.UserProfileCacheService;
import com.lebhas.creativesaas.profile.domain.UserProfile;
import com.lebhas.creativesaas.profile.infrastructure.persistence.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class SafeProfileDisplayService {

    private static final Logger log = LoggerFactory.getLogger(SafeProfileDisplayService.class);

    private final UserProfileCacheService userProfileCacheService;
    private final ProfileImageUrlCacheService profileImageUrlCacheService;
    private final UserProfileRepository userProfileRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;

    public SafeProfileDisplayService(
            UserProfileCacheService userProfileCacheService,
            ProfileImageUrlCacheService profileImageUrlCacheService,
            UserProfileRepository userProfileRepository,
            WorkspaceMembershipRepository workspaceMembershipRepository
    ) {
        this.userProfileCacheService = userProfileCacheService;
        this.profileImageUrlCacheService = profileImageUrlCacheService;
        this.userProfileRepository = userProfileRepository;
        this.workspaceMembershipRepository = workspaceMembershipRepository;
    }

    @Transactional(readOnly = true)
    public SafeProfileDisplayView forUser(UUID userId) {
        return forUserInWorkspace(null, userId);
    }

    @Transactional(readOnly = true)
    public SafeProfileDisplayView forUserInWorkspace(UUID workspaceId, UUID userId) {
        if (userId == null) {
            return null;
        }
        Role role = resolveRole(workspaceId, userId);
        try {
            return userProfileCacheService.get(userId)
                    .map(profile -> fromProfileView(profile, role))
                    .orElseGet(() -> fromProfileEntity(userId, role));
        } catch (RuntimeException ex) {
            log.warn("Unable to resolve safe profile display for userId={}", userId);
            return new SafeProfileDisplayView(userId, null, null, role);
        }
    }

    private SafeProfileDisplayView fromProfileView(UserProfileView profile, Role role) {
        return new SafeProfileDisplayView(
                profile.userId(),
                profile.displayName(),
                profile.profileImageUrl(),
                role);
    }

    private SafeProfileDisplayView fromProfileEntity(UUID userId, Role role) {
        return userProfileRepository.findByUserIdAndDeletedFalse(userId)
                .map(profile -> fromProfileEntity(profile, role))
                .orElse(new SafeProfileDisplayView(userId, null, null, role));
    }

    private SafeProfileDisplayView fromProfileEntity(UserProfile profile, Role role) {
        return new SafeProfileDisplayView(
                profile.getUserId(),
                profile.getDisplayName(),
                signedImageUrl(profile.getUserId()),
                role);
    }

    private String signedImageUrl(UUID userId) {
        Instant now = Instant.now();
        return profileImageUrlCacheService.get(userId)
                .filter(entry -> entry.expiresAt() != null && entry.expiresAt().isAfter(now))
                .map(ProfileImageUrlCacheService.ProfileImageUrlCacheEntry::imageUrl)
                .orElse(null);
    }

    private Role resolveRole(UUID workspaceId, UUID userId) {
        if (workspaceId == null || userId == null) {
            return null;
        }
        try {
            return workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndDeletedFalse(workspaceId, userId)
                    .filter(membership -> membership.getStatus() == WorkspaceMembershipStatus.ACTIVE)
                    .map(membership -> membership.getRole())
                    .orElse(null);
        } catch (RuntimeException ex) {
            log.warn("Unable to resolve safe profile display role for workspaceId={} userId={}", workspaceId, userId);
            return null;
        }
    }
}
