package com.lebhas.creativesaas.credit.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "credit_reservations", schema = "platform")
public class CreditReservationEntity extends TenantAwareEntity {

    @Column(name = "creative_request_id", nullable = false, updatable = false)
    private UUID creativeRequestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creative_request_id", nullable = false, insertable = false, updatable = false)
    private CreativeRequestEntity creativeRequest;

    @Column(name = "reserved_credits", nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedCredits;

    @Column(name = "finalized_credits", nullable = false, precision = 19, scale = 4)
    private BigDecimal finalizedCredits;

    @Column(name = "refunded_credits", nullable = false, precision = 19, scale = 4)
    private BigDecimal refundedCredits;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CreditReservationStatus status;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    protected CreditReservationEntity() {
    }

    public static CreditReservationEntity reserve(
            UUID workspaceId,
            UUID creativeRequestId,
            BigDecimal reservedCredits
    ) {
        CreditReservationEntity entity = new CreditReservationEntity();
        entity.assignWorkspace(workspaceId);
        entity.creativeRequestId = require(creativeRequestId, "creativeRequestId");
        entity.reservedCredits = normalizeCredits(reservedCredits);
        entity.finalizedCredits = zero();
        entity.refundedCredits = zero();
        entity.status = CreditReservationStatus.RESERVED;
        return entity;
    }

    public UUID getCreativeRequestId() {
        return creativeRequestId;
    }

    public CreativeRequestEntity getCreativeRequest() {
        return creativeRequest;
    }

    public BigDecimal getReservedCredits() {
        return reservedCredits;
    }

    public BigDecimal getFinalizedCredits() {
        return finalizedCredits;
    }

    public BigDecimal getRefundedCredits() {
        return refundedCredits;
    }

    public CreditReservationStatus getStatus() {
        return status;
    }

    public Instant getFinalizedAt() {
        return finalizedAt;
    }

    public void finalizeReservation(BigDecimal finalizedCredits) {
        BigDecimal normalizedFinalized = normalizeCredits(finalizedCredits);
        if (normalizedFinalized.compareTo(reservedCredits) > 0) {
            throw new IllegalArgumentException("finalizedCredits must not exceed reservedCredits");
        }
        this.finalizedCredits = normalizedFinalized;
        this.refundedCredits = reservedCredits.subtract(normalizedFinalized).setScale(4, RoundingMode.HALF_UP);
        this.status = normalizedFinalized.signum() == 0
                ? CreditReservationStatus.REFUNDED
                : CreditReservationStatus.FINALIZED;
        this.finalizedAt = Instant.now();
    }

    public void refundReservation() {
        this.finalizedCredits = zero();
        this.refundedCredits = reservedCredits;
        this.status = CreditReservationStatus.REFUNDED;
        this.finalizedAt = Instant.now();
    }

    public void cancel() {
        this.finalizedCredits = zero();
        this.refundedCredits = reservedCredits;
        this.status = CreditReservationStatus.CANCELLED;
        this.finalizedAt = Instant.now();
    }

    private static UUID require(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static BigDecimal normalizeCredits(BigDecimal value) {
        BigDecimal normalized = value == null ? zero() : value.setScale(4, RoundingMode.HALF_UP);
        if (normalized.signum() < 0) {
            throw new IllegalArgumentException("credit values must not be negative");
        }
        return normalized;
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    }
}
