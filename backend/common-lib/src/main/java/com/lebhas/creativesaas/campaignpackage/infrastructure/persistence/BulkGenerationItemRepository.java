package com.lebhas.creativesaas.campaignpackage.infrastructure.persistence;

import com.lebhas.creativesaas.campaignpackage.domain.BulkGenerationItem;
import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;

import java.util.List;
import java.util.UUID;

public interface BulkGenerationItemRepository extends TenantAwareRepository<BulkGenerationItem> {
    List<BulkGenerationItem> findAllByWorkspaceIdAndJobIdAndDeletedFalse(UUID workspaceId, UUID jobId);
}
