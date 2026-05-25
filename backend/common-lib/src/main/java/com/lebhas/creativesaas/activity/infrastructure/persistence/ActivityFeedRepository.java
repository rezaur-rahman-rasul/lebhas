package com.lebhas.creativesaas.activity.infrastructure.persistence;

import com.lebhas.creativesaas.activity.domain.ActivityFeed;
import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityFeedRepository extends TenantAwareRepository<ActivityFeed> {

    Optional<ActivityFeed> findBySourceEventIdAndDeletedFalse(String sourceEventId);

    boolean existsBySourceEventIdAndDeletedFalse(String sourceEventId);

    List<ActivityFeed> findAllByWorkspaceIdAndDeletedFalseOrderByActivityAtDesc(UUID workspaceId);

    List<ActivityFeed> findAllByWorkspaceIdAndDeletedFalseOrderByActivityAtDesc(UUID workspaceId, Pageable pageable);

    List<ActivityFeed> findAllByWorkspaceIdAndReferenceTypeAndReferenceIdAndDeletedFalseOrderByActivityAtDesc(
            UUID workspaceId,
            String referenceType,
            UUID referenceId);

    List<ActivityFeed> findAllByWorkspaceIdAndReferenceTypeAndReferenceIdAndDeletedFalseOrderByActivityAtDesc(
            UUID workspaceId,
            String referenceType,
            UUID referenceId,
            Pageable pageable);
}
