package com.lebhas.creativesaas.generation.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.credit.application.CreditWalletService;
import com.lebhas.creativesaas.credit.domain.CreditTransactionEntity;
import com.lebhas.creativesaas.credit.domain.CreditTransactionStatus;
import com.lebhas.creativesaas.credit.domain.CreditTransactionType;
import com.lebhas.creativesaas.credit.domain.CreditWalletEntity;
import com.lebhas.creativesaas.credit.infrastructure.persistence.CreditTransactionRepository;
import com.lebhas.creativesaas.generation.application.dto.CreditFinalizeCommand;
import com.lebhas.creativesaas.generation.application.dto.CreditRefundCommand;
import com.lebhas.creativesaas.generation.application.dto.CreditReservationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class CreativeCreditReservationService {

    private static final Logger log = LoggerFactory.getLogger(CreativeCreditReservationService.class);

    private final CreditWalletService creditWalletService;
    private final CreditTransactionRepository creditTransactionRepository;
    private final CreditEstimationService creditEstimationService;

    public CreativeCreditReservationService(
            CreditWalletService creditWalletService,
            CreditTransactionRepository creditTransactionRepository,
            CreditEstimationService creditEstimationService
    ) {
        this.creditWalletService = creditWalletService;
        this.creditTransactionRepository = creditTransactionRepository;
        this.creditEstimationService = creditEstimationService;
    }

    @Transactional
    public BigDecimal assertSufficientBalance(UUID workspaceId, BigDecimal estimatedCost) {
        BigDecimal requiredAmount = requirePositiveAmount(estimatedCost);
        CreditWalletEntity wallet = creditWalletService.initializeWallet(requireWorkspaceId(workspaceId));
        BigDecimal availableBalance = creditEstimationService.normalize(wallet.getAvailableBalance());
        if (availableBalance.compareTo(requiredAmount) < 0) {
            log.warn("credit_event type=balance_insufficient workspaceId={} requiredAmount={} availableAmount={}",
                    workspaceId, requiredAmount, availableBalance);
            throw new BusinessException(ErrorCode.CREDIT_BALANCE_INSUFFICIENT);
        }
        return requiredAmount;
    }

    @Transactional
    public CreditReservationResult reserveCredits(
            UUID workspaceId,
            BigDecimal estimatedCost,
            String referenceType,
            UUID referenceId
    ) {
        UUID normalizedWorkspaceId = requireWorkspaceId(workspaceId);
        UUID normalizedReferenceId = requireReferenceId(referenceId);
        String normalizedReferenceType = requireReferenceType(referenceType);
        BigDecimal requiredAmount = assertSufficientBalance(normalizedWorkspaceId, estimatedCost);

        CreditTransactionEntity reservation = creditWalletService.reserve(
                normalizedWorkspaceId,
                requiredAmount,
                normalizedReferenceType,
                normalizedReferenceId);

        CreditWalletEntity wallet = creditWalletService.initializeWallet(normalizedWorkspaceId);
        BigDecimal walletBalance = creditEstimationService.normalize(wallet.getBalance());
        BigDecimal walletReservedBalance = creditEstimationService.normalize(wallet.getReservedBalance());
        BigDecimal walletAvailableBalance = creditEstimationService.normalize(wallet.getAvailableBalance());

        log.info("credit_event type=reserved workspaceId={} reservationId={} referenceType={} referenceId={} amount={} availableBalance={}",
                normalizedWorkspaceId,
                reservation.getId(),
                normalizedReferenceType,
                normalizedReferenceId,
                requiredAmount,
                walletAvailableBalance);

        return new CreditReservationResult(
                reservation.getId(),
                normalizedWorkspaceId,
                requiredAmount,
                walletBalance,
                walletReservedBalance,
                walletAvailableBalance,
                normalizedReferenceType,
                normalizedReferenceId);
    }

    @Transactional
    public void finalizeCredits(CreditFinalizeCommand command) {
        ReservationReference reference = resolveReference(command.workspaceId(), command.creditReservationId(), command.referenceType(), command.referenceId());
        BigDecimal outstandingAmount = outstandingReservedAmount(reference);
        if (outstandingAmount.signum() <= 0) {
            log.info("credit_event type=finalize_skipped workspaceId={} reservationId={} referenceType={} referenceId={} reason={}",
                    command.workspaceId(),
                    command.creditReservationId(),
                    reference.referenceType(),
                    reference.referenceId(),
                    abbreviate(command.settlementReason()));
            return;
        }

        creditWalletService.finalizeReservation(
                reference.workspaceId(),
                outstandingAmount,
                reference.referenceType(),
                reference.referenceId());

        log.info("credit_event type=finalized workspaceId={} reservationId={} referenceType={} referenceId={} amount={} reason={}",
                reference.workspaceId(),
                command.creditReservationId(),
                reference.referenceType(),
                reference.referenceId(),
                outstandingAmount,
                abbreviate(command.settlementReason()));
    }

    @Transactional
    public void refundCredits(CreditRefundCommand command) {
        ReservationReference reference = resolveReference(command.workspaceId(), command.creditReservationId(), command.referenceType(), command.referenceId());
        BigDecimal outstandingAmount = outstandingReservedAmount(reference);
        if (outstandingAmount.signum() <= 0) {
            log.info("credit_event type=refund_skipped workspaceId={} reservationId={} referenceType={} referenceId={} reason={}",
                    command.workspaceId(),
                    command.creditReservationId(),
                    reference.referenceType(),
                    reference.referenceId(),
                    abbreviate(command.refundReason()));
            return;
        }

        creditWalletService.refund(
                reference.workspaceId(),
                outstandingAmount,
                reference.referenceType(),
                reference.referenceId());

        log.info("credit_event type=refunded workspaceId={} reservationId={} referenceType={} referenceId={} amount={} reason={}",
                reference.workspaceId(),
                command.creditReservationId(),
                reference.referenceType(),
                reference.referenceId(),
                outstandingAmount,
                abbreviate(command.refundReason()));
    }

    private ReservationReference resolveReference(
            UUID workspaceId,
            UUID creditReservationId,
            String referenceType,
            UUID referenceId
    ) {
        UUID normalizedWorkspaceId = requireWorkspaceId(workspaceId);
        if (creditReservationId != null) {
            CreditTransactionEntity reservation = creditTransactionRepository
                    .findByIdAndWorkspaceIdAndDeletedFalse(creditReservationId, normalizedWorkspaceId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CREDIT_RESERVE_INVALID, "Credit reservation was not found"));
            if (reservation.getTransactionType() != CreditTransactionType.RESERVE) {
                throw new BusinessException(ErrorCode.CREDIT_RESERVE_INVALID, "Credit transaction is not a reservation");
            }
            if (reservation.getReferenceType() == null || reservation.getReferenceId() == null) {
                throw new BusinessException(ErrorCode.CREDIT_RESERVE_INVALID, "Credit reservation is missing reference metadata");
            }
            return new ReservationReference(
                    normalizedWorkspaceId,
                    reservation.getReferenceType(),
                    reservation.getReferenceId());
        }
        return new ReservationReference(
                normalizedWorkspaceId,
                requireReferenceType(referenceType),
                requireReferenceId(referenceId));
    }

    private BigDecimal outstandingReservedAmount(ReservationReference reference) {
        List<CreditTransactionEntity> transactions = creditTransactionRepository
                .findAllByWorkspaceIdAndReferenceTypeAndReferenceIdAndDeletedFalseOrderByCreatedAtAsc(
                        reference.workspaceId(),
                        reference.referenceType(),
                        reference.referenceId());

        BigDecimal reserved = BigDecimal.ZERO;
        BigDecimal settled = BigDecimal.ZERO;

        for (CreditTransactionEntity transaction : transactions) {
            if (transaction.getStatus() != CreditTransactionStatus.COMPLETED) {
                continue;
            }
            BigDecimal amount = creditEstimationService.normalize(transaction.getAmount());
            if (transaction.getTransactionType() == CreditTransactionType.RESERVE) {
                reserved = reserved.add(amount);
            } else if (transaction.getTransactionType() == CreditTransactionType.FINALIZE
                    || transaction.getTransactionType() == CreditTransactionType.REFUND) {
                settled = settled.add(amount);
            }
        }

        BigDecimal outstanding = creditEstimationService.normalize(reserved.subtract(settled));
        if (reserved.signum() <= 0) {
            throw new BusinessException(ErrorCode.CREDIT_RESERVE_INVALID, "No credit reservation exists for the supplied reference");
        }
        return outstanding.signum() > 0 ? outstanding : creditEstimationService.normalize(BigDecimal.ZERO);
    }

    private UUID requireWorkspaceId(UUID workspaceId) {
        return Objects.requireNonNull(workspaceId, "workspaceId must not be null");
    }

    private UUID requireReferenceId(UUID referenceId) {
        return Objects.requireNonNull(referenceId, "referenceId must not be null");
    }

    private String requireReferenceType(String referenceType) {
        if (referenceType == null) {
            throw new IllegalArgumentException("referenceType must not be null");
        }
        String normalized = referenceType.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("referenceType must not be blank");
        }
        return normalized;
    }

    private BigDecimal requirePositiveAmount(BigDecimal amount) {
        BigDecimal normalized = creditEstimationService.normalize(amount);
        if (normalized.signum() <= 0) {
            throw new BusinessException(ErrorCode.CREDIT_RESERVE_INVALID, "Estimated credit amount must be greater than zero");
        }
        return normalized;
    }

    private String abbreviate(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 180 ? normalized : normalized.substring(0, 180);
    }

    private record ReservationReference(
            UUID workspaceId,
            String referenceType,
            UUID referenceId
    ) {
    }
}
