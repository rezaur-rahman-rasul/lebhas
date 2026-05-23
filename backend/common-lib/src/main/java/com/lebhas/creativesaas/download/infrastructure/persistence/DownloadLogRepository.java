package com.lebhas.creativesaas.download.infrastructure.persistence;

import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;
import com.lebhas.creativesaas.download.domain.DownloadLogEntity;

import java.util.List;
import java.util.UUID;

public interface DownloadLogRepository extends TenantAwareRepository<DownloadLogEntity> {

    List<DownloadLogEntity> findAllByGeneratedVersionIdAndDeletedFalse(UUID generatedVersionId);

    List<DownloadLogEntity> findAllByAssetIdAndDeletedFalse(UUID assetId);

    List<DownloadLogEntity> findAllByWorkspaceIdAndAssetIdAndDeletedFalseOrderByCreatedAtDesc(UUID workspaceId, UUID assetId);

    List<DownloadLogEntity> findAllByWorkspaceIdAndGeneratedVersionIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            UUID generatedVersionId
    );
}
