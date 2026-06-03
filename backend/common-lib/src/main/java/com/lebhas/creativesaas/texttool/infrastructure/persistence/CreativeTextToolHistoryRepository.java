package com.lebhas.creativesaas.texttool.infrastructure.persistence;

import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;
import com.lebhas.creativesaas.texttool.domain.CreativeTextToolHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CreativeTextToolHistoryRepository extends TenantAwareRepository<CreativeTextToolHistory> {

    Page<CreativeTextToolHistory> findAllByWorkspaceIdAndProjectIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            UUID projectId,
            Pageable pageable);
}
