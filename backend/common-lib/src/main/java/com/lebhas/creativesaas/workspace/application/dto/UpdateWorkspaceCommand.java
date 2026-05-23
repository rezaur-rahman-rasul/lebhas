package com.lebhas.creativesaas.workspace.application.dto;

import com.lebhas.creativesaas.workspace.domain.WorkspaceLanguage;
import com.lebhas.creativesaas.workspace.domain.WorkspaceStatus;

import java.util.UUID;

public record UpdateWorkspaceCommand(
        UUID workspaceId,
        String name,
        String slug,
        String logoUrl,
        String description,
        String industry,
        String timezone,
        WorkspaceLanguage language,
        String currency,
        String country,
        WorkspaceStatus status
) {
}
