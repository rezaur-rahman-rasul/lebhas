package com.lebhas.creativesaas.creative.interfaces;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConfirmAssetUploadRequest(
        @NotNull UUID assetId,
        UUID uploadReferenceId,
        String checksum
) {
}
