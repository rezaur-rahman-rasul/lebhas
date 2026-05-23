package com.lebhas.creativesaas.workspace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workspace_settings", schema = "platform")
@EntityListeners(AuditingEntityListener.class)
public class WorkspaceSettingsEntity {

    @Id
    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(name = "allow_crew_download", nullable = false)
    private boolean allowCrewDownload;

    @Column(name = "allow_crew_publish", nullable = false)
    private boolean allowCrewPublish;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_language", nullable = false, length = 20)
    private WorkspaceLanguage defaultLanguage;

    @Column(name = "default_timezone", nullable = false, length = 80)
    private String defaultTimezone;

    @Embedded
    private WorkspaceNotificationPreferences notificationPreferences;

    @Enumerated(EnumType.STRING)
    @Column(name = "workspace_visibility", nullable = false, length = 20)
    private WorkspaceVisibility workspaceVisibility;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 120)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 120)
    private String updatedBy;

    protected WorkspaceSettingsEntity() {
    }

    public static WorkspaceSettingsEntity create(UUID workspaceId, WorkspaceLanguage defaultLanguage, String defaultTimezone) {
        WorkspaceSettingsEntity settings = new WorkspaceSettingsEntity();
        settings.workspaceId = workspaceId;
        settings.allowCrewDownload = false;
        settings.allowCrewPublish = false;
        settings.defaultLanguage = defaultLanguage;
        settings.defaultTimezone = defaultTimezone == null ? null : defaultTimezone.trim();
        settings.notificationPreferences = WorkspaceNotificationPreferences.defaults();
        settings.workspaceVisibility = WorkspaceVisibility.PRIVATE;
        return settings;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public boolean isAllowCrewDownload() {
        return allowCrewDownload;
    }

    public boolean isAllowCrewPublish() {
        return allowCrewPublish;
    }

    public WorkspaceLanguage getDefaultLanguage() {
        return defaultLanguage;
    }

    public String getDefaultTimezone() {
        return defaultTimezone;
    }

    public WorkspaceNotificationPreferences getNotificationPreferences() {
        return notificationPreferences;
    }

    public WorkspaceVisibility getWorkspaceVisibility() {
        return workspaceVisibility;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(
            boolean allowCrewDownload,
            boolean allowCrewPublish,
            WorkspaceLanguage defaultLanguage,
            String defaultTimezone,
            WorkspaceNotificationPreferences notificationPreferences,
            WorkspaceVisibility workspaceVisibility
    ) {
        this.allowCrewDownload = allowCrewDownload;
        this.allowCrewPublish = allowCrewPublish;
        this.defaultLanguage = defaultLanguage;
        this.defaultTimezone = defaultTimezone == null ? null : defaultTimezone.trim();
        this.notificationPreferences = notificationPreferences;
        this.workspaceVisibility = workspaceVisibility;
    }
}
