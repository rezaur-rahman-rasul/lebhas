package com.lebhas.creativesaas.asset.application.dto;

import com.lebhas.creativesaas.asset.domain.AssetCategory;
import com.lebhas.creativesaas.asset.domain.AssetType;
import com.lebhas.creativesaas.asset.domain.AssetStatus;
import com.lebhas.creativesaas.asset.domain.PreviewStatus;
import com.lebhas.creativesaas.asset.domain.ProcessingStatus;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.UUID;

public record AssetListCriteria(
        UUID workspaceId,
        UUID projectId,
        AssetType assetType,
        AssetCategory assetCategory,
        PreviewStatus previewStatus,
        ProcessingStatus processingStatus,
        UUID uploadedBy,
        AssetStatus status,
        String keyword,
        Instant createdFrom,
        Instant createdTo,
        int page,
        int size,
        String sortBy,
        Sort.Direction sortDirection
) {
}
