package com.lebhas.creativesaas.asset.application.dto;

import java.util.UUID;

public record CreateAssetFolderCommand(
        UUID workspaceId,
        String name,
        UUID parentFolderId,
        String description
) {
}
