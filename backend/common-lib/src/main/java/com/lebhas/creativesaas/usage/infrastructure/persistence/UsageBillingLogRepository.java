package com.lebhas.creativesaas.usage.infrastructure.persistence;

import com.lebhas.creativesaas.usage.domain.UsageBillingLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsageBillingLogRepository extends JpaRepository<UsageBillingLog, UUID> {

    List<UsageBillingLog> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    Page<UsageBillingLog> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId, Pageable pageable);

    Page<UsageBillingLog> findAllByEstimatedCostUsdIsNotNullOrderByCreatedAtDesc(Pageable pageable);

    Optional<UsageBillingLog> findByWorkspaceIdAndReferenceTypeAndReferenceId(UUID workspaceId, String referenceType, UUID referenceId);

    List<UsageBillingLog> findAllByReferenceTypeAndReferenceId(String referenceType, UUID referenceId);

    default List<UsageBillingLog> findAllByGeneratedVersionIdAndDeletedFalse(UUID generatedVersionId) {
        return findAllByReferenceTypeAndReferenceId("GENERATED_VERSION", generatedVersionId);
    }
}
