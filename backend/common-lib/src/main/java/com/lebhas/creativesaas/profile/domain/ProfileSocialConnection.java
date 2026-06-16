package com.lebhas.creativesaas.profile.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(
        name = "profile_social_connections",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(name = "uk_profile_social_connection_user_provider", columnNames = {"user_id", "provider"})
)
public class ProfileSocialConnection extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "profile_url", nullable = false, length = 500)
    private String profileUrl;

    protected ProfileSocialConnection() {
    }

    public static ProfileSocialConnection create(UUID userId, String provider, String profileUrl) {
        ProfileSocialConnection connection = new ProfileSocialConnection();
        connection.userId = userId;
        connection.provider = provider;
        connection.profileUrl = profileUrl.trim();
        return connection;
    }

    public UUID getUserId() { return userId; }
    public String getProvider() { return provider; }
    public String getProfileUrl() { return profileUrl; }

    public void updateProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl.trim();
    }
}
