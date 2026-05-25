package com.lebhas.creativesaas.activity.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "activity_feed_entries",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(name = "uk_activity_feed_source_event_id", columnNames = "source_event_id"),
        indexes = {
                @Index(name = "idx_activity_feed_workspace_created_at", columnList = "workspace_id,activity_at"),
                @Index(name = "idx_activity_feed_actor_created_at", columnList = "actor_user_id,activity_at"),
                @Index(name = "idx_activity_feed_reference", columnList = "reference_type,reference_id"),
                @Index(name = "idx_activity_feed_category", columnList = "activity_category")
        })
public class ActivityFeed extends TenantAwareEntity {

    @Column(name = "source_event_id", nullable = false, updatable = false, length = 120)
    private String sourceEventId;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_category", nullable = false, length = 40)
    private ActivityCategory activityCategory;

    @Column(name = "activity_type", nullable = false, length = 120)
    private String activityType;

    @Column(name = "title", nullable = false, length = 180)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "reference_type", length = 80)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "activity_at", nullable = false)
    private Instant activityAt;

    protected ActivityFeed() {
    }

    public static ActivityFeed create(
            UUID workspaceId,
            String sourceEventId,
            UUID actorUserId,
            ActivityCategory activityCategory,
            String activityType,
            String title,
            String description,
            String referenceType,
            UUID referenceId,
            Instant activityAt
    ) {
        ActivityFeed entry = new ActivityFeed();
        entry.assignWorkspace(workspaceId);
        entry.sourceEventId = normalizeRequired(sourceEventId, "sourceEventId");
        entry.actorUserId = actorUserId;
        entry.activityCategory = require(activityCategory, "activityCategory");
        entry.activityType = normalizeRequired(activityType, "activityType");
        entry.title = normalizeRequired(title, "title");
        entry.description = normalizeNullable(description);
        entry.referenceType = normalizeNullable(referenceType);
        entry.referenceId = referenceId;
        entry.activityAt = activityAt == null ? Instant.now() : activityAt;
        return entry;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public ActivityCategory getActivityCategory() {
        return activityCategory;
    }

    public String getActivityType() {
        return activityType;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public Instant getActivityAt() {
        return activityAt;
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
