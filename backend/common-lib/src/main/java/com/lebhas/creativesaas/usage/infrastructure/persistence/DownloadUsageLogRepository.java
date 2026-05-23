package com.lebhas.creativesaas.usage.infrastructure.persistence;

import com.lebhas.creativesaas.usage.domain.DownloadUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DownloadUsageLogRepository extends JpaRepository<DownloadUsageLog, UUID> {

    List<DownloadUsageLog> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    List<DownloadUsageLog> findAllByGeneratedVersionId(UUID generatedVersionId);

    List<DownloadUsageLog> findAllByAssetId(UUID assetId);
}
