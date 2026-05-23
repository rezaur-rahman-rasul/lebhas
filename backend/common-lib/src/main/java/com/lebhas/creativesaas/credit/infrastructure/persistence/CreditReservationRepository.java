package com.lebhas.creativesaas.credit.infrastructure.persistence;

import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;
import com.lebhas.creativesaas.credit.domain.CreditReservationEntity;
import com.lebhas.creativesaas.credit.domain.CreditReservationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditReservationRepository extends TenantAwareRepository<CreditReservationEntity> {

    Optional<CreditReservationEntity> findByIdAndWorkspaceIdAndDeletedFalse(UUID id, UUID workspaceId);

    Optional<CreditReservationEntity> findFirstByWorkspaceIdAndCreativeRequestIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            UUID creativeRequestId
    );

    List<CreditReservationEntity> findAllByWorkspaceIdAndCreativeRequestIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            UUID creativeRequestId
    );

    List<CreditReservationEntity> findAllByWorkspaceIdAndStatusAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            CreditReservationStatus status
    );
}
