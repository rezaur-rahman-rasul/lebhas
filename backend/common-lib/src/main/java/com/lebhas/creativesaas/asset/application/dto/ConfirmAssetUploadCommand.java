package com.lebhas.creativesaas.asset.application.dto;

import java.util.UUID;

public record ConfirmAssetUploadCommand(
        UUID workspaceId,
        UUID assetId,
        UUID uploadReferenceId,
        String checksum
) {
}
