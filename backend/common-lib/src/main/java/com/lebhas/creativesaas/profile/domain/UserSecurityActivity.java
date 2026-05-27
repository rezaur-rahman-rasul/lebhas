package com.lebhas.creativesaas.profile.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "user_security_activities", schema = "platform")
public class UserSecurityActivity extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 40)
    private UserSecurityActivityType activityType;

    @Column(name = "ip_address", length = 80)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "location_hint", length = 160)
    private String locationHint;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "failure_reason", length = 300)
    private String failureReason;

    protected UserSecurityActivity() {
    }

    public static UserSecurityActivity record(
            UUID userId,
            UserSecurityActivityType activityType,
            String ipAddress,
            String userAgent,
            String locationHint,
            boolean success,
            String failureReason
    ) {
        UserSecurityActivity activity = new UserSecurityActivity();
        activity.userId = requireUserId(userId);
        activity.activityType = requireActivityType(activityType);
        activity.ipAddress = trimToNull(ipAddress);
        activity.userAgent = trimToNull(userAgent);
        activity.locationHint = trimToNull(locationHint);
        activity.success = success;
        activity.failureReason = success ? null : trimToNull(failureReason);
        return activity;
    }

    public UUID getUserId() {
        return userId;
    }

    public UserSecurityActivityType getActivityType() {
        return activityType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getLocationHint() {
        return locationHint;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getFailureReason() {
        return failureReason;
    }

    private static UUID requireUserId(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        return userId;
    }

    private static UserSecurityActivityType requireActivityType(UserSecurityActivityType activityType) {
        if (activityType == null) {
            throw new IllegalArgumentException("activityType must not be null");
        }
        return activityType;
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
