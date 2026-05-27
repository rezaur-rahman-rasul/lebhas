package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.identity.domain.UserStatus;
import com.lebhas.creativesaas.identity.infrastructure.persistence.UserRepository;
import com.lebhas.creativesaas.profile.application.ProfileImageStorageService.SignedProfileImageUrl;
import com.lebhas.creativesaas.profile.application.ProfileImageUploadSessionService.ProfileImageUploadSession;
import com.lebhas.creativesaas.profile.application.dto.ChangePasswordRequest;
import com.lebhas.creativesaas.profile.application.dto.ConfirmProfileImageUploadRequest;
import com.lebhas.creativesaas.profile.application.dto.ProfileImageUploadUrlRequest;
import com.lebhas.creativesaas.profile.application.dto.UpdateAccountSettingsRequest;
import com.lebhas.creativesaas.profile.application.dto.UpdateProfileRequest;
import com.lebhas.creativesaas.profile.cache.ProfileImageUrlCacheService;
import com.lebhas.creativesaas.profile.cache.ProfileLockService;
import com.lebhas.creativesaas.profile.cache.ProfileRateLimitService;
import com.lebhas.creativesaas.profile.cache.UserAccountSettingsCacheService;
import com.lebhas.creativesaas.profile.cache.UserProfileCacheService;
import com.lebhas.creativesaas.profile.domain.PreferredLanguage;
import com.lebhas.creativesaas.profile.domain.ThemePreference;
import com.lebhas.creativesaas.profile.domain.UserAccountSettings;
import com.lebhas.creativesaas.profile.domain.UserProfile;
import com.lebhas.creativesaas.profile.domain.UserSecurityActivityType;
import com.lebhas.creativesaas.profile.event.ProfileEventProducer;
import com.lebhas.creativesaas.profile.infrastructure.persistence.UserAccountSettingsRepository;
import com.lebhas.creativesaas.profile.infrastructure.persistence.UserProfileRepository;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.asset.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileManagementBatch12UnitTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID WORKSPACE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-27T00:00:00Z"), ZoneOffset.UTC);

    private CurrentUserContext currentUserContext;
    private UserProfileQueryService queryService;
    private UserProfileRepository profileRepository;
    private UserAccountSettingsRepository settingsRepository;
    private UserProfileCacheService profileCache;
    private UserAccountSettingsCacheService settingsCache;
    private ProfileImageUrlCacheService imageUrlCache;
    private ProfileRateLimitService rateLimitService;
    private ProfileLockService lockService;
    private ProfileEventProducer eventProducer;
    private ProfileNotificationActivityAuditIntegration integration;

    @BeforeEach
    void setUp() {
        currentUserContext = mock(CurrentUserContext.class);
        queryService = mock(UserProfileQueryService.class);
        profileRepository = mock(UserProfileRepository.class);
        settingsRepository = mock(UserAccountSettingsRepository.class);
        profileCache = mock(UserProfileCacheService.class);
        settingsCache = mock(UserAccountSettingsCacheService.class);
        imageUrlCache = mock(ProfileImageUrlCacheService.class);
        rateLimitService = mock(ProfileRateLimitService.class);
        lockService = mock(ProfileLockService.class);
        eventProducer = mock(ProfileEventProducer.class);
        integration = mock(ProfileNotificationActivityAuditIntegration.class);
        when(currentUserContext.requireCurrentUser()).thenReturn(currentUser(Role.ADMIN));
        when(rateLimitService.incrementProfileUpdate(USER_ID)).thenReturn(allowed());
        when(rateLimitService.incrementPasswordChange(USER_ID)).thenReturn(allowed());
        when(lockService.acquireProfileUpdateLock(USER_ID)).thenReturn(Optional.of(lock("lock:profile:update:" + USER_ID)));
        when(lockService.acquirePasswordLock(USER_ID)).thenReturn(Optional.of(lock("lock:profile:password:" + USER_ID)));
        when(lockService.acquireProfileImageLock(USER_ID)).thenReturn(Optional.of(lock("lock:profile:image:" + USER_ID)));
        when(lockService.releaseQuietly(any())).thenReturn(true);
    }

    @Test
    void profileCreatedOnRegistration() {
        UserProfileRepository repository = mock(UserProfileRepository.class);
        when(repository.findByUserIdAndDeletedFalse(USER_ID)).thenReturn(Optional.empty());
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile profile = new UserProfileProvisioningService(repository, new ProfileDefaultsFactory())
                .provisionIfMissing(user(USER_ID, "Ariana", "Rahman", "hash"));

        assertThat(profile.getUserId()).isEqualTo(USER_ID);
        assertThat(profile.getDisplayName()).isEqualTo("Ariana Rahman");
        verify(repository).save(any(UserProfile.class));
    }

    @Test
    void profileCreatedOnWorkspaceAdminOnboarding() {
        UserAccountSettingsRepository repository = mock(UserAccountSettingsRepository.class);
        when(repository.findByUserIdAndDeletedFalse(USER_ID)).thenReturn(Optional.empty());
        when(repository.save(any(UserAccountSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserAccountSettings settings = new UserAccountSettingsProvisioningService(repository, new ProfileDefaultsFactory())
                .provisionIfMissing(user(USER_ID, "Admin", "Owner", "hash"));

        assertThat(settings.getPreferredLanguage()).isEqualTo(PreferredLanguage.BOTH);
        assertThat(settings.getThemePreference()).isEqualTo(ThemePreference.SYSTEM);
    }

    @Test
    void getProfileMeReturnsCurrentUserProfile() {
        var expected = mapper().toView(profile(USER_ID), settings(USER_ID));
        when(queryService.viewOwnProfile()).thenReturn(expected);

        assertThat(profileService().viewOwnProfile()).isSameAs(expected);
    }

    @Test
    void putProfileMeUpdatesOnlyOwnProfile() {
        UserProfile profile = profile(USER_ID);
        when(queryService.requireProfile(USER_ID)).thenReturn(profile);
        when(queryService.requireAccountSettings(USER_ID)).thenReturn(settings(USER_ID));
        when(profileRepository.save(profile)).thenReturn(profile);

        var view = profileService().updateOwnProfile(updateProfileRequest(), "127.0.0.1", "JUnit");

        assertThat(view.userId()).isEqualTo(USER_ID);
        assertThat(view.displayName()).isEqualTo("Ariana R.");
        verify(queryService).requireProfile(USER_ID);
        verify(queryService, never()).requireProfile(OTHER_USER_ID);
    }

    @Test
    void userCannotUpdateAnotherUserProfile() {
        UserProfile profile = profile(USER_ID);
        when(queryService.requireProfile(USER_ID)).thenReturn(profile);
        when(queryService.requireAccountSettings(USER_ID)).thenReturn(settings(USER_ID));
        when(profileRepository.save(profile)).thenReturn(profile);

        profileService().updateOwnProfile(updateProfileRequest());

        verify(queryService, never()).requireProfile(OTHER_USER_ID);
    }

    @Test
    void profileSettingsUpdateWorks() {
        UserAccountSettings settings = settings(USER_ID);
        when(queryService.requireAccountSettings(USER_ID)).thenReturn(settings);
        when(settingsRepository.save(settings)).thenReturn(settings);

        var view = settingsService().updateOwnAccountSettings(settingsRequest(PreferredLanguage.ENGLISH));

        assertThat(view.themePreference()).isEqualTo(ThemePreference.DARK);
        verify(profileCache).invalidate(USER_ID);
        verify(settingsCache).invalidate(USER_ID);
    }

    @Test
    void preferredLanguageUpdateWorks() {
        UserAccountSettings settings = settings(USER_ID);
        when(queryService.requireAccountSettings(USER_ID)).thenReturn(settings);
        when(settingsRepository.save(settings)).thenReturn(settings);

        assertThat(settingsService().updateOwnAccountSettings(settingsRequest(PreferredLanguage.BANGLA)).preferredLanguage())
                .isEqualTo(PreferredLanguage.BANGLA);
    }

    @Test
    void passwordChangeSucceedsWithValidCurrentPasswordAndCreatesHistory() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        UserRepository userRepository = mock(UserRepository.class);
        UserPasswordHistoryService historyService = mock(UserPasswordHistoryService.class);
        UserSecurityActivityService activityService = mock(UserSecurityActivityService.class);
        SessionRevocationService revocationService = mock(SessionRevocationService.class);
        PasswordStrengthPolicyService strengthPolicy = mock(PasswordStrengthPolicyService.class);
        UserEntity user = user(USER_ID, "Ariana", "Rahman", encoder.encode("Current!234"));
        when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(revocationService.revokeOtherSessions(any())).thenReturn(new SessionRevocationService.RevocationResult(1, Set.of("device-2")));
        doNothing().when(strengthPolicy).validate("NextPassword!234");
        doNothing().when(historyService).assertNotReused(eq(USER_ID), eq("NextPassword!234"), any());

        passwordService(userRepository, encoder, strengthPolicy, historyService, activityService, revocationService)
                .changeOwnPassword(new ChangePasswordRequest("Current!234", "NextPassword!234", "NextPassword!234", true));

        assertThat(encoder.matches("NextPassword!234", user.getPassword())).isTrue();
        verify(historyService).record(eq(USER_ID), any());
        verify(eventProducer).profilePasswordChanged(WORKSPACE_ID, USER_ID, USER_ID, true, 1);
    }

    @Test
    void passwordChangeFailsWithWrongCurrentPassword() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        UserRepository userRepository = mock(UserRepository.class);
        UserSecurityActivityService activityService = mock(UserSecurityActivityService.class);
        when(userRepository.findByIdAndDeletedFalse(USER_ID))
                .thenReturn(Optional.of(user(USER_ID, "Ariana", "Rahman", encoder.encode("Current!234"))));

        assertThatThrownBy(() -> passwordService(userRepository, encoder, mock(PasswordStrengthPolicyService.class),
                mock(UserPasswordHistoryService.class), activityService, mock(SessionRevocationService.class))
                .changeOwnPassword(new ChangePasswordRequest("Wrong!234", "NextPassword!234", "NextPassword!234", false)))
                .isInstanceOf(BusinessException.class);
        verify(activityService).record(eq(USER_ID), eq(UserSecurityActivityType.PASSWORD_CHANGED), eq(null), eq(null), eq(null), eq(false), eq("current_password_invalid"));
    }

    @Test
    void passwordChangeFailsWhenConfirmPasswordMismatch() {
        assertThatThrownBy(() -> passwordService(mock(UserRepository.class), new BCryptPasswordEncoder(),
                mock(PasswordStrengthPolicyService.class), mock(UserPasswordHistoryService.class),
                mock(UserSecurityActivityService.class), mock(SessionRevocationService.class))
                .changeOwnPassword(new ChangePasswordRequest("Current!234", "NextPassword!234", "Mismatch!234", false)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("confirmation");
    }

    @Test
    void passwordHashNeverExposed() {
        assertThat(mapper().toView(profile(USER_ID), settings(USER_ID)).toString())
                .doesNotContain("password", "hash", "Current!234");
    }

    @Test
    void passwordHistoryEntryCreated() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        UserRepository userRepository = mock(UserRepository.class);
        UserPasswordHistoryService historyService = mock(UserPasswordHistoryService.class);
        when(userRepository.findByIdAndDeletedFalse(USER_ID))
                .thenReturn(Optional.of(user(USER_ID, "Ariana", "Rahman", encoder.encode("Current!234"))));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        passwordService(userRepository, encoder, mock(PasswordStrengthPolicyService.class), historyService,
                mock(UserSecurityActivityService.class), mock(SessionRevocationService.class))
                .changeOwnPassword(new ChangePasswordRequest("Current!234", "NextPassword!234", "NextPassword!234", false));

        verify(historyService).record(eq(USER_ID), any());
    }

    @Test
    void profileImageUploadUrlGenerated() {
        ProfileImageStorageService storage = mock(ProfileImageStorageService.class);
        ProfileImageUploadSessionService sessions = mock(ProfileImageUploadSessionService.class);
        when(storage.createUploadUrl(any(), eq("image/png"), eq(1024L)))
                .thenReturn(new SignedProfileImageUrl("https://signed-upload.example", CLOCK.instant().plusSeconds(900)));
        when(sessions.create(eq(USER_ID), any(), eq("avatar.png"), eq("image/png"), eq("png"), eq(1024L), any()))
                .thenReturn(uploadSession("profiles/" + USER_ID + "/new.png"));

        var response = imageService(storage, sessions)
                .requestSignedUploadUrl(new ProfileImageUploadUrlRequest("avatar.png", "image/png", 1024L));

        assertThat(response.uploadUrl()).isEqualTo("https://signed-upload.example");
        assertThat(response.toString()).doesNotContain("profiles/", "objectKey", "bucket");
    }

    @Test
    void invalidProfileImageMimeRejected() {
        assertThatThrownBy(() -> new ProfileImageValidationService(1024)
                .validate(new ProfileImageUploadUrlRequest("avatar.gif", "image/gif", 100)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void oversizedProfileImageRejected() {
        assertThatThrownBy(() -> new ProfileImageValidationService(100)
                .validate(new ProfileImageUploadUrlRequest("avatar.png", "image/png", 101)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void profileImageConfirmUpdatesProfile() {
        ProfileImageStorageService storage = mock(ProfileImageStorageService.class);
        ProfileImageUploadSessionService sessions = mock(ProfileImageUploadSessionService.class);
        UserProfile profile = profile(USER_ID);
        when(sessions.require(any(), eq(USER_ID))).thenReturn(uploadSession("profiles/" + USER_ID + "/new.png"));
        when(storage.metadata(any())).thenReturn(new StorageService.StoredObjectMetadata(1024L, CLOCK.instant()));
        when(storage.createPreviewUrl(any(), any()))
                .thenReturn(new SignedProfileImageUrl("https://signed-preview.example", CLOCK.instant().plusSeconds(600)));
        when(queryService.requireProfile(USER_ID)).thenReturn(profile);
        when(queryService.requireAccountSettings(USER_ID)).thenReturn(settings(USER_ID));
        when(profileRepository.save(profile)).thenReturn(profile);

        var view = imageService(storage, sessions).confirmUpload(new ConfirmProfileImageUploadRequest(UUID.randomUUID()));

        assertThat(view.profileImageUrl()).isEqualTo("https://signed-preview.example");
        verify(imageUrlCache).cache(eq(USER_ID), eq("https://signed-preview.example"), any());
    }

    @Test
    void profileImageDeleteWorks() {
        UserProfile profile = profile(USER_ID);
        profile.updateProfileImage(null, "profiles/" + USER_ID + "/old.png", "https://old.example");
        ProfileImageStorageService storage = mock(ProfileImageStorageService.class);
        when(queryService.requireProfile(USER_ID)).thenReturn(profile);
        when(queryService.requireAccountSettings(USER_ID)).thenReturn(settings(USER_ID));
        when(profileRepository.save(profile)).thenReturn(profile);

        var view = imageService(storage, mock(ProfileImageUploadSessionService.class)).removeProfileImage();

        assertThat(view.profileImageUrl()).isNull();
        assertThat(profile.getProfileImageObjectKey()).isNull();
        verify(storage).deleteQuietly("profiles/" + USER_ID + "/old.png");
    }

    @Test
    void rawR2ObjectKeyNotExposed() {
        UserProfile profile = profile(USER_ID);
        profile.updateProfileImage(null, "profiles/" + USER_ID + "/new.png", "https://signed-preview.example");

        assertThat(mapper().toView(profile, settings(USER_ID), "https://signed-preview.example", CLOCK.instant()).toString())
                .doesNotContain("profiles/" + USER_ID, "objectKey", "bucket");
    }

    @Test
    void kafkaProfileUpdatedEventPublished() {
        UserProfile profile = profile(USER_ID);
        when(queryService.requireProfile(USER_ID)).thenReturn(profile);
        when(queryService.requireAccountSettings(USER_ID)).thenReturn(settings(USER_ID));
        when(profileRepository.save(profile)).thenReturn(profile);

        profileService().updateOwnProfile(updateProfileRequest());

        verify(eventProducer).profileUpdated(WORKSPACE_ID, profile.getId(), USER_ID, USER_ID);
    }

    @Test
    void kafkaProfileImageUpdatedEventPublished() {
        ProfileImageStorageService storage = mock(ProfileImageStorageService.class);
        ProfileImageUploadSessionService sessions = mock(ProfileImageUploadSessionService.class);
        UserProfile profile = profile(USER_ID);
        when(sessions.require(any(), eq(USER_ID))).thenReturn(uploadSession("profiles/" + USER_ID + "/new.png"));
        when(storage.metadata(any())).thenReturn(new StorageService.StoredObjectMetadata(1024L, CLOCK.instant()));
        when(storage.createPreviewUrl(any(), any())).thenReturn(new SignedProfileImageUrl("https://signed-preview.example", CLOCK.instant()));
        when(queryService.requireProfile(USER_ID)).thenReturn(profile);
        when(queryService.requireAccountSettings(USER_ID)).thenReturn(settings(USER_ID));
        when(profileRepository.save(profile)).thenReturn(profile);

        imageService(storage, sessions).confirmUpload(new ConfirmProfileImageUploadRequest(UUID.randomUUID()));

        verify(eventProducer).profileImageUpdated(WORKSPACE_ID, profile.getId(), USER_ID, USER_ID);
    }

    @Test
    void kafkaProfilePasswordChangedEventPublished() {
        passwordChangeSucceedsWithValidCurrentPasswordAndCreatesHistory();
    }

    @Test
    void profileUpdateCreatesAuditLogHook() {
        UserProfile profile = profile(USER_ID);
        when(queryService.requireProfile(USER_ID)).thenReturn(profile);
        when(queryService.requireAccountSettings(USER_ID)).thenReturn(settings(USER_ID));
        when(profileRepository.save(profile)).thenReturn(profile);

        profileService().updateOwnProfile(updateProfileRequest(), "127.0.0.1", "JUnit");

        verify(integration).profileUpdated(currentUser(Role.ADMIN), profile.getId(), "127.0.0.1", "JUnit");
    }

    @Test
    void passwordChangeCreatesSecurityActivity() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        UserRepository userRepository = mock(UserRepository.class);
        UserSecurityActivityService activityService = mock(UserSecurityActivityService.class);
        when(userRepository.findByIdAndDeletedFalse(USER_ID))
                .thenReturn(Optional.of(user(USER_ID, "Ariana", "Rahman", encoder.encode("Current!234"))));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        passwordService(userRepository, encoder, mock(PasswordStrengthPolicyService.class),
                mock(UserPasswordHistoryService.class), activityService, mock(SessionRevocationService.class))
                .changeOwnPassword(new ChangePasswordRequest("Current!234", "NextPassword!234", "NextPassword!234", false));

        verify(activityService).record(eq(USER_ID), eq(UserSecurityActivityType.PASSWORD_CHANGED), eq(null), eq(null), eq(null), eq(true), eq(null));
    }

    @Test
    void profileImageUpdateCreatesActivityFeedHook() {
        ProfileImageStorageService storage = mock(ProfileImageStorageService.class);
        ProfileImageUploadSessionService sessions = mock(ProfileImageUploadSessionService.class);
        UserProfile profile = profile(USER_ID);
        when(sessions.require(any(), eq(USER_ID))).thenReturn(uploadSession("profiles/" + USER_ID + "/new.png"));
        when(storage.metadata(any())).thenReturn(new StorageService.StoredObjectMetadata(1024L, CLOCK.instant()));
        when(storage.createPreviewUrl(any(), any())).thenReturn(new SignedProfileImageUrl("https://signed-preview.example", CLOCK.instant()));
        when(queryService.requireProfile(USER_ID)).thenReturn(profile);
        when(queryService.requireAccountSettings(USER_ID)).thenReturn(settings(USER_ID));
        when(profileRepository.save(profile)).thenReturn(profile);

        imageService(storage, sessions).confirmUpload(new ConfirmProfileImageUploadRequest(UUID.randomUUID()), "127.0.0.1", "JUnit");

        verify(integration).profileImageUpdated(currentUser(Role.ADMIN), profile.getId(), "127.0.0.1", "JUnit");
    }

    @Test
    void masterCanViewSafeUserProfileMetadata() {
        CurrentUserContext masterContext = mock(CurrentUserContext.class);
        UserRepository userRepository = mock(UserRepository.class);
        when(masterContext.requireCurrentUser()).thenReturn(currentUser(Role.MASTER));
        when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(user(USER_ID, "Ariana", "Rahman", "hash")));
        when(profileRepository.findByUserIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(profile(USER_ID)));
        when(settingsRepository.findByUserIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(settings(USER_ID)));

        var view = new MasterUserProfileSupportService(masterContext, userRepository, profileRepository, settingsRepository, mapper())
                .viewUserProfileMetadata(USER_ID);

        assertThat(view.maskedEmail()).contains("***");
        assertThat(view.toString()).doesNotContain("hash", "password", "token");
    }

    @Test
    void nonMasterCannotAccessMasterUserProfileApiService() {
        UserRepository userRepository = mock(UserRepository.class);

        assertThatThrownBy(() -> new MasterUserProfileSupportService(currentUserContext, userRepository, profileRepository, settingsRepository, mapper())
                .viewUserProfileMetadata(USER_ID))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void workspaceIsolationRemainsIntactByUsingCurrentWorkspaceContext() {
        UserProfile profile = profile(USER_ID);
        when(queryService.requireProfile(USER_ID)).thenReturn(profile);
        when(queryService.requireAccountSettings(USER_ID)).thenReturn(settings(USER_ID));
        when(profileRepository.save(profile)).thenReturn(profile);

        profileService().updateOwnProfile(updateProfileRequest());

        verify(eventProducer).profileUpdated(eq(WORKSPACE_ID), eq(profile.getId()), eq(USER_ID), eq(USER_ID));
    }

    @Test
    void standardApiResponseFormatWorks() {
        var response = ApiResponse.success("ok", mapper().toView(profile(USER_ID), settings(USER_ID)));

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("ok");
        assertThat(response.errors()).isEmpty();
        assertThat(response.timestamp()).isNotNull();
    }

    private UserProfileService profileService() {
        UserProfileService service = new UserProfileService(currentUserContext, queryService, profileRepository, mapper(),
                profileCache, settingsCache, rateLimitService, lockService, eventProducer);
        service.setProfileIntegration(integration);
        return service;
    }

    private UserAccountSettingsService settingsService() {
        return new UserAccountSettingsService(currentUserContext, queryService, settingsRepository, new UserAccountSettingsMapper(),
                profileCache, settingsCache, rateLimitService, lockService, eventProducer);
    }

    private ProfilePasswordService passwordService(UserRepository userRepository, PasswordEncoder encoder,
                                                   PasswordStrengthPolicyService strengthPolicy,
                                                   UserPasswordHistoryService historyService,
                                                   UserSecurityActivityService activityService,
                                                   SessionRevocationService revocationService) {
        return new ProfilePasswordService(currentUserContext, userRepository, encoder, strengthPolicy, historyService,
                activityService, revocationService, rateLimitService, lockService, CLOCK, eventProducer);
    }

    private ProfileImageService imageService(ProfileImageStorageService storage, ProfileImageUploadSessionService sessions) {
        ProfileImageService service = new ProfileImageService(currentUserContext, new ProfileImageValidationService(5 * 1024 * 1024L),
                storage, sessions, queryService, profileRepository, mapper(), imageUrlCache, profileCache, lockService, CLOCK, eventProducer);
        service.setProfileIntegration(integration);
        return service;
    }

    private static UserProfileMapper mapper() {
        return new UserProfileMapper(new UserAccountSettingsMapper());
    }

    private static UpdateProfileRequest updateProfileRequest() {
        return new UpdateProfileRequest("Ariana", "Rahman", "Ariana R.", "+8801700000000", "Designer", "Asia/Dhaka", "en-BD");
    }

    private static UpdateAccountSettingsRequest settingsRequest(PreferredLanguage language) {
        return new UpdateAccountSettingsRequest(language, ThemePreference.DARK, true, true, false);
    }

    private static UserProfile profile(UUID userId) {
        UserProfile profile = UserProfile.create(userId, "Ariana", "Rahman", "Ariana Rahman", "+8801700000000", "Designer", null, null, null, "Asia/Dhaka", "en");
        setId(profile, UUID.randomUUID());
        return profile;
    }

    private static UserAccountSettings settings(UUID userId) {
        UserAccountSettings settings = UserAccountSettings.create(userId, PreferredLanguage.BOTH, ThemePreference.SYSTEM, true, true, false);
        setId(settings, UUID.randomUUID());
        return settings;
    }

    private static UserEntity user(UUID userId, String firstName, String lastName, String passwordHash) {
        UserEntity user = UserEntity.register(firstName, lastName, "ariana@example.com", "+8801700000000", passwordHash, Role.ADMIN, UserStatus.ACTIVE, true);
        setId(user, userId);
        return user;
    }

    private static CurrentUser currentUser(Role role) {
        return new CurrentUser(USER_ID, WORKSPACE_ID, "device-1", "ariana@example.com", Set.of(role), Set.of(Permission.WORKSPACE_VIEW), "token-id", CLOCK.instant().plusSeconds(900));
    }

    private static ProfileRateLimitService.RateLimitDecision allowed() {
        return new ProfileRateLimitService.RateLimitDecision(1, 20, true, Duration.ofMinutes(1), false);
    }

    private static RedisLockService.RedisLockToken lock(String key) {
        return new RedisLockService.RedisLockToken(key, "token", CLOCK.instant().plusSeconds(15));
    }

    private static ProfileImageUploadSession uploadSession(String objectKey) {
        return new ProfileImageUploadSession(UUID.randomUUID(), USER_ID, objectKey, "avatar.png", "image/png", "png", 1024L, CLOCK.instant(), CLOCK.instant().plusSeconds(900));
    }

    private static void setId(BaseEntity entity, UUID id) {
        try {
            Field field = BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
