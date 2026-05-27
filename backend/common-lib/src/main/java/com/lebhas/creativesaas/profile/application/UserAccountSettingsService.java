package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.profile.application.dto.UpdateAccountSettingsRequest;
import com.lebhas.creativesaas.profile.application.dto.UserProfileView;
import com.lebhas.creativesaas.profile.cache.ProfileLockService;
import com.lebhas.creativesaas.profile.cache.ProfileRateLimitService;
import com.lebhas.creativesaas.profile.cache.UserAccountSettingsCacheService;
import com.lebhas.creativesaas.profile.cache.UserProfileCacheService;
import com.lebhas.creativesaas.profile.domain.UserAccountSettings;
import com.lebhas.creativesaas.profile.event.ProfileEventProducer;
import com.lebhas.creativesaas.profile.infrastructure.persistence.UserAccountSettingsRepository;
import com.lebhas.creativesaas.redis.RedisLockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserAccountSettingsService {

    private final CurrentUserContext currentUserContext;
    private final UserProfileQueryService userProfileQueryService;
    private final UserAccountSettingsRepository userAccountSettingsRepository;
    private final UserAccountSettingsMapper userAccountSettingsMapper;
    private final UserProfileCacheService userProfileCacheService;
    private final UserAccountSettingsCacheService userAccountSettingsCacheService;
    private final ProfileRateLimitService profileRateLimitService;
    private final ProfileLockService profileLockService;
    private final ProfileEventProducer profileEventProducer;
    private ProfileNotificationActivityAuditIntegration profileIntegration;

    public UserAccountSettingsService(
            CurrentUserContext currentUserContext,
            UserProfileQueryService userProfileQueryService,
            UserAccountSettingsRepository userAccountSettingsRepository,
            UserAccountSettingsMapper userAccountSettingsMapper,
            UserProfileCacheService userProfileCacheService,
            UserAccountSettingsCacheService userAccountSettingsCacheService,
            ProfileRateLimitService profileRateLimitService,
            ProfileLockService profileLockService,
            ProfileEventProducer profileEventProducer
    ) {
        this.currentUserContext = currentUserContext;
        this.userProfileQueryService = userProfileQueryService;
        this.userAccountSettingsRepository = userAccountSettingsRepository;
        this.userAccountSettingsMapper = userAccountSettingsMapper;
        this.userProfileCacheService = userProfileCacheService;
        this.userAccountSettingsCacheService = userAccountSettingsCacheService;
        this.profileRateLimitService = profileRateLimitService;
        this.profileLockService = profileLockService;
        this.profileEventProducer = profileEventProducer;
    }

    @Autowired(required = false)
    void setProfileIntegration(ProfileNotificationActivityAuditIntegration profileIntegration) {
        this.profileIntegration = profileIntegration;
    }

    @Transactional
    public UserProfileView.AccountSettingsView viewOwnAccountSettings() {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        return userAccountSettingsCacheService.get(currentUser.userId())
                .orElseGet(() -> {
                    UserAccountSettings settings = userProfileQueryService.requireAccountSettings(currentUser.userId());
                    UserProfileView.AccountSettingsView view = userAccountSettingsMapper.toView(settings);
                    userAccountSettingsCacheService.cache(currentUser.userId(), view);
                    return view;
                });
    }

    @Transactional
    public UserProfileView.AccountSettingsView updateOwnAccountSettings(UpdateAccountSettingsRequest request) {
        return updateOwnAccountSettings(request, null, null);
    }

    @Transactional
    public UserProfileView.AccountSettingsView updateOwnAccountSettings(
            UpdateAccountSettingsRequest request,
            String ipAddress,
            String userAgent
    ) {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        validate(request);
        validateRateLimit(currentUser.userId());
        RedisLockService.RedisLockToken lockToken = profileLockService.acquireProfileUpdateLock(currentUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Profile update is already in progress"));
        try {
            UserAccountSettings settings = userProfileQueryService.requireAccountSettings(currentUser.userId());
            settings.update(
                    request.preferredLanguage(),
                    request.themePreference(),
                    request.notificationEmailEnabled(),
                    request.notificationInAppEnabled(),
                    request.marketingEmailEnabled());
            UserAccountSettings saved = userAccountSettingsRepository.save(settings);
            userProfileCacheService.invalidate(currentUser.userId());
            userAccountSettingsCacheService.invalidate(currentUser.userId());
            publishSettingsUpdated(currentUser, saved);
            integrateSettingsUpdated(currentUser, saved.getId(), ipAddress, userAgent);
            UserProfileView.AccountSettingsView view = userAccountSettingsMapper.toView(saved);
            userAccountSettingsCacheService.cache(currentUser.userId(), view);
            return view;
        } finally {
            profileLockService.releaseQuietly(lockToken);
        }
    }

    private void validate(UpdateAccountSettingsRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Account settings request is required");
        }
        if (request.preferredLanguage() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "preferredLanguage is required");
        }
        if (request.themePreference() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "themePreference is required");
        }
    }

    private void validateRateLimit(UUID userId) {
        ProfileRateLimitService.RateLimitDecision decision = profileRateLimitService.incrementProfileUpdate(userId);
        if (!decision.allowed()) {
            throw new BusinessException(ErrorCode.AUTH_RATE_LIMITED, "Too many profile update attempts. Try again later.");
        }
    }

    private void publishSettingsUpdated(CurrentUser currentUser, UserAccountSettings settings) {
        profileEventProducer.profileSettingsUpdated(currentUser.workspaceId(), settings.getId(), currentUser.userId(), currentUser.userId());
    }

    private void integrateSettingsUpdated(CurrentUser currentUser, UUID settingsId, String ipAddress, String userAgent) {
        if (profileIntegration != null) {
            profileIntegration.settingsUpdated(currentUser, settingsId, ipAddress, userAgent);
        }
    }
}
