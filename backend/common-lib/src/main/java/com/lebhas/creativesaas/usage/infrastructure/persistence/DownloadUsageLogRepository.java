package com.lebhas.creativesaas.usage.infrastructure.persistence;

import com.lebhas.creativesaas.usage.domain.DownloadUsageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DownloadUsageLogRepository extends JpaRepository<DownloadUsageLog, UUID> {

    List<DownloadUsageLog> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    Page<DownloadUsageLog> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId, Pageable pageable);

    List<DownloadUsageLog> findAllByGeneratedVersionId(UUID generatedVersionId);

    List<DownloadUsageLog> findAllByAssetId(UUID assetId);
}
