package com.lebhas.creativesaas.asset.application.dto;

import java.time.Instant;
import java.util.UUID;

public record AssetFolderView(
        UUID id,
        UUID workspaceId,
        String name,
        UUID parentFolderId,
        String description,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
