package com.lebhas.creativesaas.workspace.application.dto;

import com.lebhas.creativesaas.workspace.domain.WorkspaceLanguage;
import com.lebhas.creativesaas.workspace.domain.WorkspaceVisibility;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceSettingsView(
        UUID workspaceId,
        boolean allowCrewDownload,
        boolean allowCrewPublish,
        WorkspaceLanguage defaultLanguage,
        String defaultTimezone,
        NotificationPreferencesView notificationPreferences,
        WorkspaceVisibility workspaceVisibility,
        Instant createdAt,
        Instant updatedAt
) {
}
