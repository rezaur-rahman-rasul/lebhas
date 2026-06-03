package com.lebhas.creativesaas.creative.interfaces;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.lebhas.creativesaas.asset.domain.AssetCategory;
import com.lebhas.creativesaas.asset.domain.AssetType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record CreateAssetUploadUrlRequest(
        AssetType assetType,
        @JsonAlias("category")
        @NotNull AssetCategory assetCategory,
        UUID folderId,
        @JsonAlias("fileName")
        @NotBlank String originalFileName,
        @NotBlank String contentType,
        @JsonAlias("fileSize")
        @Min(1) long sizeBytes,
        String checksum,
        String displayName,
        String description,
        Set<String> tags,
        Map<String, Object> metadata
) {
}
