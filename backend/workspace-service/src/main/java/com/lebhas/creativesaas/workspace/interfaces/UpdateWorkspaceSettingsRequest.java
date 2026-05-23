package com.lebhas.creativesaas.workspace.interfaces;

import com.lebhas.creativesaas.common.validation.ValidationMessages;
import com.lebhas.creativesaas.common.validation.timezone.ValidTimezone;
import com.lebhas.creativesaas.workspace.domain.WorkspaceLanguage;
import com.lebhas.creativesaas.workspace.domain.WorkspaceVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateWorkspaceSettingsRequest(
        boolean allowCrewDownload,
        boolean allowCrewPublish,
        @NotNull(message = ValidationMessages.REQUIRED)
        WorkspaceLanguage defaultLanguage,
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Size(max = 80)
        @ValidTimezone(message = ValidationMessages.INVALID_TIMEZONE)
        String defaultTimezone,
        @Valid
        @NotNull(message = ValidationMessages.REQUIRED)
        NotificationPreferencesRequest notificationPreferences,
        @NotNull(message = ValidationMessages.REQUIRED)
        WorkspaceVisibility workspaceVisibility
) {
}
