package com.lebhas.creativesaas.usage.infrastructure.persistence;

import com.lebhas.creativesaas.usage.domain.ShareUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShareUsageLogRepository extends JpaRepository<ShareUsageLog, UUID> {

    List<ShareUsageLog> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    List<ShareUsageLog> findAllByShareLinkId(UUID shareLinkId);

    List<ShareUsageLog> findAllByGeneratedVersionId(UUID generatedVersionId);
}
