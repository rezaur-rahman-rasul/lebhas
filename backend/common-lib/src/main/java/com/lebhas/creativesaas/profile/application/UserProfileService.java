package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.identity.infrastructure.persistence.UserRepository;
import com.lebhas.creativesaas.profile.application.dto.UpdateProfileRequest;
import com.lebhas.creativesaas.profile.application.dto.UserProfileView;
import com.lebhas.creativesaas.profile.cache.ProfileLockService;
import com.lebhas.creativesaas.profile.cache.ProfileRateLimitService;
import com.lebhas.creativesaas.profile.cache.UserAccountSettingsCacheService;
import com.lebhas.creativesaas.profile.cache.UserProfileCacheService;
import com.lebhas.creativesaas.profile.domain.UserAccountSettings;
import com.lebhas.creativesaas.profile.domain.UserProfile;
import com.lebhas.creativesaas.profile.event.ProfileEventProducer;
import com.lebhas.creativesaas.profile.infrastructure.persistence.UserProfileRepository;
import com.lebhas.creativesaas.redis.RedisLockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UserProfileService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9][0-9 .()\\-]{6,29}$");

    private final CurrentUserContext currentUserContext;
    private final UserProfileQueryService userProfileQueryService;
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final UserProfileMapper userProfileMapper;
    private final UserProfileCacheService userProfileCacheService;
    private final UserAccountSettingsCacheService userAccountSettingsCacheService;
    private final ProfileRateLimitService profileRateLimitService;
    private final ProfileLockService profileLockService;
    private final ProfileEventProducer profileEventProducer;
    private final ProfileImageService profileImageService;
    private ProfileNotificationActivityAuditIntegration profileIntegration;

    public UserProfileService(
            CurrentUserContext currentUserContext,
            UserProfileQueryService userProfileQueryService,
            UserProfileRepository userProfileRepository,
            UserRepository userRepository,
            UserProfileMapper userProfileMapper,
            UserProfileCacheService userProfileCacheService,
            UserAccountSettingsCacheService userAccountSettingsCacheService,
            ProfileRateLimitService profileRateLimitService,
            ProfileLockService profileLockService,
            ProfileEventProducer profileEventProducer,
            ProfileImageService profileImageService
    ) {
        this.currentUserContext = currentUserContext;
        this.userProfileQueryService = userProfileQueryService;
        this.userProfileRepository = userProfileRepository;
        this.userRepository = userRepository;
        this.userProfileMapper = userProfileMapper;
        this.userProfileCacheService = userProfileCacheService;
        this.userAccountSettingsCacheService = userAccountSettingsCacheService;
        this.profileRateLimitService = profileRateLimitService;
        this.profileLockService = profileLockService;
        this.profileEventProducer = profileEventProducer;
        this.profileImageService = profileImageService;
    }

    @Autowired(required = false)
    void setProfileIntegration(ProfileNotificationActivityAuditIntegration profileIntegration) {
        this.profileIntegration = profileIntegration;
    }

    @Transactional(readOnly = true)
    public UserProfileView viewOwnProfile() {
        return userProfileQueryService.viewOwnProfile();
    }

    @Transactional
    public UserProfileView updateOwnProfile(UpdateProfileRequest request) {
        return updateOwnProfile(request, null, null);
    }

    @Transactional
    public UserProfileView updateOwnProfile(UpdateProfileRequest request, String ipAddress, String userAgent) {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        validateRateLimit(currentUser.userId());
        RedisLockService.RedisLockToken lockToken = profileLockService.acquireProfileUpdateLock(currentUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Profile update is already in progress"));
        try {
            ValidatedProfileUpdate validated = validate(request);
            UserProfile profile = userProfileQueryService.requireProfile(currentUser.userId());
            profile.updateProfile(
                    validated.firstName(),
                    validated.lastName(),
                    validated.displayName(),
                    validated.phoneNumber(),
                    validated.jobTitle(),
                    validated.timezone(),
                    validated.locale());
            UserProfile saved = userProfileRepository.save(profile);
            syncIdentityUser(currentUser.userId(), saved);
            invalidateCaches(currentUser.userId());
            publishProfileUpdated(currentUser, saved);
            integrateProfileUpdated(currentUser, saved.getId(), ipAddress, userAgent);
            UserAccountSettings settings = userProfileQueryService.requireAccountSettings(currentUser.userId());
            UserProfileView view = userProfileMapper.toView(saved, settings, currentUser.email());
            userProfileCacheService.cache(currentUser.userId(), view);
            if (view.accountSettings() != null) {
                userAccountSettingsCacheService.cache(currentUser.userId(), view.accountSettings());
            }
            return view;
        } finally {
            profileLockService.releaseQuietly(lockToken);
        }
    }

    @Transactional
    public UserProfileView updateOwnProfile(
            UpdateProfileRequest request,
            MultipartFile profileImage,
            String ipAddress,
            String userAgent
    ) {
        UserProfileView updated = updateOwnProfile(request, ipAddress, userAgent);
        if (profileImage == null || profileImage.isEmpty()) {
            return updated;
        }
        return profileImageService.uploadDirect(profileImage, ipAddress, userAgent);
    }

    private void validateRateLimit(UUID userId) {
        ProfileRateLimitService.RateLimitDecision decision = profileRateLimitService.incrementProfileUpdate(userId);
        if (!decision.allowed()) {
            throw new BusinessException(ErrorCode.AUTH_RATE_LIMITED, "Too many profile update attempts. Try again later.");
        }
    }

    private ValidatedProfileUpdate validate(UpdateProfileRequest request) {
        if (request == null) {
            throw validation("Profile update request is required");
        }
        String firstName = required(request.firstName(), "firstName", 80);
        String lastName = required(request.lastName(), "lastName", 80);
        String displayName = required(request.displayName(), "displayName", 160);
        String phoneNumber = optional(request.phoneNumber(), "phoneNumber", 30);
        String jobTitle = optional(request.jobTitle(), "jobTitle", 120);
        String timezone = required(request.timezone(), "timezone", 80);
        String locale = required(request.locale(), "locale", 20);
        validatePhone(phoneNumber);
        validateTimezone(timezone);
        locale = validateLocale(locale);
        return new ValidatedProfileUpdate(firstName, lastName, displayName, phoneNumber, jobTitle, timezone, locale);
    }

    private static String required(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw validation(fieldName + " is required");
        }
        return optional(value, fieldName, maxLength);
    }

    private static String optional(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw validation(fieldName + " must be at most " + maxLength + " characters");
        }
        return trimmed;
    }

    private static void validatePhone(String phoneNumber) {
        if (phoneNumber != null && !PHONE_PATTERN.matcher(phoneNumber).matches()) {
            throw validation("phoneNumber format is invalid");
        }
    }

    private static void validateTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            throw validation("timezone format is invalid");
        }
    }

    private static String validateLocale(String locale) {
        String normalized = locale.replace('_', '-');
        Locale parsed = Locale.forLanguageTag(normalized);
        if (parsed.getLanguage() == null || parsed.getLanguage().isBlank() || "und".equalsIgnoreCase(parsed.getLanguage())) {
            throw validation("locale format is invalid");
        }
        return parsed.toLanguageTag().toLowerCase(Locale.ROOT);
    }

    private static BusinessException validation(String message) {
        return new BusinessException(ErrorCode.VALIDATION_FAILED, message);
    }

    private void invalidateCaches(UUID userId) {
        userProfileCacheService.invalidate(userId);
        userAccountSettingsCacheService.invalidate(userId);
    }

    private void publishProfileUpdated(CurrentUser currentUser, UserProfile profile) {
        profileEventProducer.profileUpdated(currentUser.workspaceId(), profile.getId(), currentUser.userId(), currentUser.userId());
    }

    private void integrateProfileUpdated(CurrentUser currentUser, UUID profileId, String ipAddress, String userAgent) {
        if (profileIntegration != null) {
            profileIntegration.profileUpdated(currentUser, profileId, ipAddress, userAgent);
        }
    }

    private void syncIdentityUser(UUID userId, UserProfile profile) {
        userRepository.findByIdAndDeletedFalse(userId).ifPresent(user -> {
            user.updateProfile(profile.getFirstName(), profile.getLastName(), user.getEmail(), profile.getPhoneNumber());
            userRepository.save(user);
        });
    }

    private record ValidatedProfileUpdate(
            String firstName,
            String lastName,
            String displayName,
            String phoneNumber,
            String jobTitle,
            String timezone,
            String locale
    ) {
    }
}
