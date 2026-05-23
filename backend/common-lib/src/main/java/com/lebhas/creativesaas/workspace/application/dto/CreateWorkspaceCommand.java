package com.lebhas.creativesaas.workspace.application.dto;

import com.lebhas.creativesaas.workspace.domain.WorkspaceLanguage;

public record CreateWorkspaceCommand(
        String name,
        String slug,
        String logoUrl,
        String description,
        String industry,
        String timezone,
        WorkspaceLanguage language,
        String currency,
        String country
) {
}
