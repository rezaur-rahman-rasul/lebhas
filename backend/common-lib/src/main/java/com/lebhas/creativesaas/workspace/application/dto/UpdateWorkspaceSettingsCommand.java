package com.lebhas.creativesaas.workspace.application.dto;

import com.lebhas.creativesaas.workspace.domain.WorkspaceLanguage;
import com.lebhas.creativesaas.workspace.domain.WorkspaceVisibility;

import java.util.UUID;

public record UpdateWorkspaceSettingsCommand(
        UUID workspaceId,
        boolean allowCrewDownload,
        boolean allowCrewPublish,
        WorkspaceLanguage defaultLanguage,
        String defaultTimezone,
        NotificationPreferencesView notificationPreferences,
        WorkspaceVisibility workspaceVisibility
) {
}
