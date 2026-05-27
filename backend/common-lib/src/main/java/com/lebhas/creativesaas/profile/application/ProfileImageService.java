package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.profile.application.ProfileImageStorageService.SignedProfileImageUrl;
import com.lebhas.creativesaas.profile.application.ProfileImageUploadSessionService.ProfileImageUploadSession;
import com.lebhas.creativesaas.profile.application.ProfileImageValidationService.ValidatedProfileImageUpload;
import com.lebhas.creativesaas.profile.application.dto.ConfirmProfileImageUploadRequest;
import com.lebhas.creativesaas.profile.application.dto.ProfileImageUploadUrlRequest;
import com.lebhas.creativesaas.profile.application.dto.ProfileImageUploadUrlResponse;
import com.lebhas.creativesaas.profile.application.dto.UserProfileView;
import com.lebhas.creativesaas.profile.cache.ProfileImageUrlCacheService;
import com.lebhas.creativesaas.profile.cache.ProfileLockService;
import com.lebhas.creativesaas.profile.cache.UserProfileCacheService;
import com.lebhas.creativesaas.profile.domain.UserAccountSettings;
import com.lebhas.creativesaas.profile.domain.UserProfile;
import com.lebhas.creativesaas.profile.event.ProfileEventProducer;
import com.lebhas.creativesaas.profile.infrastructure.persistence.UserProfileRepository;
import com.lebhas.creativesaas.redis.RedisLockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class ProfileImageService {

    private final CurrentUserContext currentUserContext;
    private final ProfileImageValidationService validationService;
    private final ProfileImageStorageService storageService;
    private final ProfileImageUploadSessionService uploadSessionService;
    private final UserProfileQueryService userProfileQueryService;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;
    private final ProfileImageUrlCacheService profileImageUrlCacheService;
    private final UserProfileCacheService userProfileCacheService;
    private final ProfileLockService profileLockService;
    private final Clock clock;
    private final ProfileEventProducer profileEventProducer;
    private ProfileNotificationActivityAuditIntegration profileIntegration;

    public ProfileImageService(
            CurrentUserContext currentUserContext,
            ProfileImageValidationService validationService,
            ProfileImageStorageService storageService,
            ProfileImageUploadSessionService uploadSessionService,
            UserProfileQueryService userProfileQueryService,
            UserProfileRepository userProfileRepository,
            UserProfileMapper userProfileMapper,
            ProfileImageUrlCacheService profileImageUrlCacheService,
            UserProfileCacheService userProfileCacheService,
            ProfileLockService profileLockService,
            Clock clock,
            ProfileEventProducer profileEventProducer
    ) {
        this.currentUserContext = currentUserContext;
        this.validationService = validationService;
        this.storageService = storageService;
        this.uploadSessionService = uploadSessionService;
        this.userProfileQueryService = userProfileQueryService;
        this.userProfileRepository = userProfileRepository;
        this.userProfileMapper = userProfileMapper;
        this.profileImageUrlCacheService = profileImageUrlCacheService;
        this.userProfileCacheService = userProfileCacheService;
        this.profileLockService = profileLockService;
        this.clock = clock;
        this.profileEventProducer = profileEventProducer;
    }

    @Autowired(required = false)
    void setProfileIntegration(ProfileNotificationActivityAuditIntegration profileIntegration) {
        this.profileIntegration = profileIntegration;
    }

    public ProfileImageUploadUrlResponse requestSignedUploadUrl(ProfileImageUploadUrlRequest request) {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        RedisLockService.RedisLockToken lockToken = profileLockService.acquireProfileImageLock(currentUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Profile image update is already in progress"));
        try {
            ValidatedProfileImageUpload validated = validationService.validate(request);
            String objectKey = objectKey(currentUser.userId(), validated.extension());
            SignedProfileImageUrl signedUrl = storageService.createUploadUrl(
                    objectKey,
                    validated.mimeType(),
                    validated.fileSize());
            ProfileImageUploadSession session = uploadSessionService.create(
                    currentUser.userId(),
                    objectKey,
                    validated.fileName(),
                    validated.mimeType(),
                    validated.extension(),
                    validated.fileSize(),
                    signedUrl.expiresAt());
            ProfileImageUploadUrlResponse response = new ProfileImageUploadUrlResponse(
                    session.uploadReferenceId(),
                    signedUrl.url(),
                    signedUrl.expiresAt(),
                    validated.maxFileSize(),
                    "PUT");
            profileEventProducer.profileImageUploadRequested(
                    currentUser.workspaceId(),
                    session.uploadReferenceId(),
                    currentUser.userId(),
                    currentUser.userId(),
                    session.mimeType(),
                    session.fileSize(),
                    session.extension(),
                    signedUrl.expiresAt());
            integrateProfileImageUploadRequested(currentUser, session);
            return response;
        } finally {
            profileLockService.releaseQuietly(lockToken);
        }
    }

    @Transactional
    public UserProfileView confirmUpload(ConfirmProfileImageUploadRequest request) {
        return confirmUpload(request, null, null);
    }

    @Transactional
    public UserProfileView confirmUpload(ConfirmProfileImageUploadRequest request, String ipAddress, String userAgent) {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        RedisLockService.RedisLockToken lockToken = profileLockService.acquireProfileImageLock(currentUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Profile image update is already in progress"));
        try {
            if (request == null || request.uploadReferenceId() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "uploadReferenceId is required");
            }
            ProfileImageUploadSession session = uploadSessionService.require(request.uploadReferenceId(), currentUser.userId());
            validationService.validateStoredObject(storageService.metadata(session.objectKey()).contentLength(), session.fileSize());
            UserProfile profile = userProfileQueryService.requireProfile(currentUser.userId());
            String oldObjectKey = profile.getProfileImageObjectKey();
            SignedProfileImageUrl preview = storageService.createPreviewUrl(session.objectKey(), session.mimeType());
            profile.updateProfileImage(null, session.objectKey(), preview.url());
            UserProfile saved = userProfileRepository.save(profile);
            uploadSessionService.delete(session.uploadReferenceId());
            invalidateCaches(currentUser.userId());
            profileImageUrlCacheService.cache(currentUser.userId(), preview.url(), preview.expiresAt());
            cleanupOldObject(oldObjectKey, session.objectKey());
            profileEventProducer.profileImageUpdated(currentUser.workspaceId(), saved.getId(), currentUser.userId(), currentUser.userId());
            integrateProfileImageUpdated(currentUser, saved.getId(), ipAddress, userAgent);
            return profileView(saved, preview);
        } finally {
            profileLockService.releaseQuietly(lockToken);
        }
    }

    @Transactional
    public UserProfileView removeProfileImage() {
        return removeProfileImage(null, null);
    }

    @Transactional
    public UserProfileView removeProfileImage(String ipAddress, String userAgent) {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        RedisLockService.RedisLockToken lockToken = profileLockService.acquireProfileImageLock(currentUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Profile image update is already in progress"));
        try {
            UserProfile profile = userProfileQueryService.requireProfile(currentUser.userId());
            String oldObjectKey = profile.getProfileImageObjectKey();
            profile.clearProfileImage();
            UserProfile saved = userProfileRepository.save(profile);
            invalidateCaches(currentUser.userId());
            cleanupOldObject(oldObjectKey, null);
            profileEventProducer.profileImageRemoved(currentUser.workspaceId(), saved.getId(), currentUser.userId(), currentUser.userId());
            integrateProfileImageRemoved(currentUser, saved.getId(), ipAddress, userAgent);
            return profileView(saved, null);
        } finally {
            profileLockService.releaseQuietly(lockToken);
        }
    }

    @Transactional(readOnly = true)
    public UserProfileView signedProfileImagePreview() {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        UserProfile profile = userProfileQueryService.requireProfile(currentUser.userId());
        if (profile.getProfileImageObjectKey() == null || profile.getProfileImageObjectKey().isBlank()) {
            return profileView(profile, null);
        }
        SignedProfileImageUrl signedUrl = profileImageUrlCacheService.get(currentUser.userId())
                .filter(entry -> entry.expiresAt() != null && entry.expiresAt().isAfter(clock.instant()))
                .map(entry -> new SignedProfileImageUrl(entry.imageUrl(), entry.expiresAt()))
                .orElseGet(() -> {
                    SignedProfileImageUrl generated = storageService.createPreviewUrl(
                            profile.getProfileImageObjectKey(),
                            mimeTypeFromObjectKey(profile.getProfileImageObjectKey()));
                    profileImageUrlCacheService.cache(currentUser.userId(), generated.url(), generated.expiresAt());
                    return generated;
                });
        return profileView(profile, signedUrl);
    }

    private UserProfileView profileView(UserProfile profile, SignedProfileImageUrl signedUrl) {
        UserAccountSettings settings = userProfileQueryService.requireAccountSettings(profile.getUserId());
        return userProfileMapper.toView(
                profile,
                settings,
                signedUrl == null ? null : signedUrl.url(),
                signedUrl == null ? null : signedUrl.expiresAt());
    }

    private void invalidateCaches(UUID userId) {
        userProfileCacheService.invalidate(userId);
        profileImageUrlCacheService.invalidate(userId);
    }

    private void cleanupOldObject(String oldObjectKey, String newObjectKey) {
        if (oldObjectKey != null && !oldObjectKey.isBlank() && !oldObjectKey.equals(newObjectKey)) {
            storageService.deleteQuietly(oldObjectKey);
        }
    }

    private void integrateProfileImageUploadRequested(CurrentUser currentUser, ProfileImageUploadSession session) {
        if (profileIntegration != null) {
            profileIntegration.profileImageUploadRequested(
                    currentUser,
                    session.uploadReferenceId(),
                    session.mimeType(),
                    session.fileSize(),
                    session.extension());
        }
    }

    private void integrateProfileImageUpdated(CurrentUser currentUser, UUID profileId, String ipAddress, String userAgent) {
        if (profileIntegration != null) {
            profileIntegration.profileImageUpdated(currentUser, profileId, ipAddress, userAgent);
        }
    }

    private void integrateProfileImageRemoved(CurrentUser currentUser, UUID profileId, String ipAddress, String userAgent) {
        if (profileIntegration != null) {
            profileIntegration.profileImageRemoved(currentUser, profileId, ipAddress, userAgent);
        }
    }

    private static String objectKey(UUID userId, String extension) {
        return "profiles/" + userId + "/" + UUID.randomUUID() + "." + extension;
    }

    private static String mimeTypeFromObjectKey(String objectKey) {
        String lower = objectKey.toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

}
