package com.lebhas.creativesaas.asset.application.dto;

import java.util.UUID;

public record UpdateAssetFolderCommand(
        UUID workspaceId,
        UUID folderId,
        String name,
        UUID parentFolderId,
        String description
) {
}
