package com.lebhas.creativesaas.profile.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Locale;
import java.util.UUID;

@Entity
@Table(
        name = "user_profiles",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_profiles_user_id", columnNames = "user_id")
)
public class UserProfile extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "job_title", length = 120)
    private String jobTitle;

    @Column(name = "profile_image_asset_id")
    private UUID profileImageAssetId;

    @Column(name = "profile_image_object_key", length = 500)
    private String profileImageObjectKey;

    @Column(name = "profile_image_url_cached", length = 1000)
    private String profileImageUrlCached;

    @Column(name = "timezone", nullable = false, length = 80)
    private String timezone;

    @Column(name = "locale", nullable = false, length = 20)
    private String locale;

    protected UserProfile() {
    }

    public static UserProfile create(
            UUID userId,
            String firstName,
            String lastName,
            String displayName,
            String phoneNumber,
            String jobTitle,
            UUID profileImageAssetId,
            String profileImageObjectKey,
            String profileImageUrlCached,
            String timezone,
            String locale
    ) {
        UserProfile profile = new UserProfile();
        profile.userId = requireUserId(userId);
        profile.firstName = requireText(firstName, "firstName");
        profile.lastName = requireText(lastName, "lastName");
        profile.displayName = requireText(displayName, "displayName");
        profile.phoneNumber = trimToNull(phoneNumber);
        profile.jobTitle = trimToNull(jobTitle);
        profile.profileImageAssetId = profileImageAssetId;
        profile.profileImageObjectKey = trimToNull(profileImageObjectKey);
        profile.profileImageUrlCached = trimToNull(profileImageUrlCached);
        profile.timezone = requireText(timezone, "timezone");
        profile.locale = normalizeLocale(locale);
        return profile;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public UUID getProfileImageAssetId() {
        return profileImageAssetId;
    }

    public String getProfileImageObjectKey() {
        return profileImageObjectKey;
    }

    public String getProfileImageUrlCached() {
        return profileImageUrlCached;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getLocale() {
        return locale;
    }

    public void updateProfile(
            String firstName,
            String lastName,
            String displayName,
            String phoneNumber,
            String jobTitle,
            String timezone,
            String locale
    ) {
        this.firstName = requireText(firstName, "firstName");
        this.lastName = requireText(lastName, "lastName");
        this.displayName = requireText(displayName, "displayName");
        this.phoneNumber = trimToNull(phoneNumber);
        this.jobTitle = trimToNull(jobTitle);
        this.timezone = requireText(timezone, "timezone");
        this.locale = normalizeLocale(locale);
    }

    public void updateProfileImage(UUID assetId, String objectKey, String urlCached) {
        this.profileImageAssetId = assetId;
        this.profileImageObjectKey = trimToNull(objectKey);
        this.profileImageUrlCached = trimToNull(urlCached);
    }

    public void clearProfileImage() {
        this.profileImageAssetId = null;
        this.profileImageObjectKey = null;
        this.profileImageUrlCached = null;
    }

    private static UUID requireUserId(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        return userId;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeLocale(String locale) {
        return requireText(locale, "locale").toLowerCase(Locale.ROOT);
    }
}
