package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipStatus;
import com.lebhas.creativesaas.identity.infrastructure.persistence.UserRepository;
import com.lebhas.creativesaas.identity.infrastructure.persistence.WorkspaceMembershipRepository;
import com.lebhas.creativesaas.profile.application.dto.UserProfileView;
import com.lebhas.creativesaas.profile.cache.ProfileImageUrlCacheService;
import com.lebhas.creativesaas.profile.cache.UserAccountSettingsCacheService;
import com.lebhas.creativesaas.profile.cache.UserProfileCacheService;
import com.lebhas.creativesaas.profile.domain.UserAccountSettings;
import com.lebhas.creativesaas.profile.domain.UserProfile;
import com.lebhas.creativesaas.profile.infrastructure.persistence.UserAccountSettingsRepository;
import com.lebhas.creativesaas.profile.infrastructure.persistence.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserProfileQueryService {

    private final CurrentUserContext currentUserContext;
    private final UserRepository userRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserAccountSettingsRepository userAccountSettingsRepository;
    private final UserProfileProvisioningService userProfileProvisioningService;
    private final UserAccountSettingsProvisioningService userAccountSettingsProvisioningService;
    private final UserProfileMapper userProfileMapper;
    private final UserProfileCacheService userProfileCacheService;
    private final UserAccountSettingsCacheService userAccountSettingsCacheService;
    private final ProfileImageUrlCacheService profileImageUrlCacheService;

    public UserProfileQueryService(
            CurrentUserContext currentUserContext,
            UserRepository userRepository,
            WorkspaceAuthorizationService workspaceAuthorizationService,
            WorkspaceMembershipRepository workspaceMembershipRepository,
            UserProfileRepository userProfileRepository,
            UserAccountSettingsRepository userAccountSettingsRepository,
            UserProfileProvisioningService userProfileProvisioningService,
            UserAccountSettingsProvisioningService userAccountSettingsProvisioningService,
            UserProfileMapper userProfileMapper,
            UserProfileCacheService userProfileCacheService,
            UserAccountSettingsCacheService userAccountSettingsCacheService,
            ProfileImageUrlCacheService profileImageUrlCacheService
    ) {
        this.currentUserContext = currentUserContext;
        this.userRepository = userRepository;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.workspaceMembershipRepository = workspaceMembershipRepository;
        this.userProfileRepository = userProfileRepository;
        this.userAccountSettingsRepository = userAccountSettingsRepository;
        this.userProfileProvisioningService = userProfileProvisioningService;
        this.userAccountSettingsProvisioningService = userAccountSettingsProvisioningService;
        this.userProfileMapper = userProfileMapper;
        this.userProfileCacheService = userProfileCacheService;
        this.userAccountSettingsCacheService = userAccountSettingsCacheService;
        this.profileImageUrlCacheService = profileImageUrlCacheService;
    }

    @Transactional
    public UserProfileView viewOwnProfile() {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        return getProfileView(currentUser.userId());
    }

    @Transactional
    public UserProfileView getWorkspaceProfileMetadata(UUID workspaceId, UUID userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "userId is required");
        }
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceContext(workspaceId);
        boolean activeMember = workspaceMembershipRepository.existsByUserIdAndWorkspaceIdAndStatusAndDeletedFalse(
                userId,
                access.workspace().getId(),
                WorkspaceMembershipStatus.ACTIVE);
        if (!activeMember) {
            throw new BusinessException(ErrorCode.WORKSPACE_MEMBER_NOT_FOUND);
        }
        return getProfileView(userId);
    }

    UserProfileView getProfileView(UUID userId) {
        return userProfileCacheService.get(userId)
                .orElseGet(() -> loadAndCacheProfileView(userId));
    }

    UserProfile requireProfile(UUID userId) {
        return userProfileRepository.findByUserIdAndDeletedFalse(userId)
                .orElseGet(() -> userProfileProvisioningService.provisionIfMissing(requireUser(userId)));
    }

    UserAccountSettings requireAccountSettings(UUID userId) {
        return userAccountSettingsRepository.findByUserIdAndDeletedFalse(userId)
                .orElseGet(() -> userAccountSettingsProvisioningService.provisionIfMissing(requireUser(userId)));
    }

    private UserProfileView loadAndCacheProfileView(UUID userId) {
        UserProfile profile = requireProfile(userId);
        UserAccountSettings settings = requireAccountSettings(userId);
        ProfileImageUrlCacheService.ProfileImageUrlCacheEntry imageUrl = profileImageUrlCacheService.get(userId)
                .filter(entry -> entry.expiresAt() != null && entry.expiresAt().isAfter(java.time.Instant.now()))
                .orElse(null);
        UserProfileView view = imageUrl == null
                ? userProfileMapper.toView(profile, settings)
                : userProfileMapper.toView(profile, settings, imageUrl.imageUrl(), imageUrl.expiresAt());
        userProfileCacheService.cache(userId, view);
        if (view.accountSettings() != null) {
            userAccountSettingsCacheService.cache(userId, view.accountSettings());
        }
        return view;
    }

    private UserEntity requireUser(UUID userId) {
        return userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
    }
}
