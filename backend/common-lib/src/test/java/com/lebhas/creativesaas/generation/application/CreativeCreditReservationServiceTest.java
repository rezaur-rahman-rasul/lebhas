package com.lebhas.creativesaas.generation.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.credit.application.CreditWalletService;
import com.lebhas.creativesaas.credit.domain.CreditTransactionEntity;
import com.lebhas.creativesaas.credit.domain.CreditTransactionStatus;
import com.lebhas.creativesaas.credit.domain.CreditTransactionType;
import com.lebhas.creativesaas.credit.domain.CreditWalletEntity;
import com.lebhas.creativesaas.credit.infrastructure.persistence.CreditTransactionRepository;
import com.lebhas.creativesaas.generation.application.dto.CreditFinalizeCommand;
import com.lebhas.creativesaas.generation.application.dto.CreditRefundCommand;
import com.lebhas.creativesaas.generation.application.dto.CreditReservationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreativeCreditReservationServiceTest {

    private final CreditWalletService creditWalletService = mock(CreditWalletService.class);
    private final CreditTransactionRepository creditTransactionRepository = mock(CreditTransactionRepository.class);

    private CreativeCreditReservationService service;

    @BeforeEach
    void setUp() {
        service = new CreativeCreditReservationService(
                creditWalletService,
                creditTransactionRepository,
                new CreditEstimationService());
    }

    @Test
    void shouldRejectReservationWhenAvailableBalanceIsInsufficient() {
        UUID workspaceId = UUID.randomUUID();
        CreditWalletEntity wallet = wallet(workspaceId, "0.5000", "0.0000");
        when(creditWalletService.initializeWallet(workspaceId)).thenReturn(wallet);

        assertThatThrownBy(() -> service.reserveCredits(
                workspaceId,
                new BigDecimal("1.0000"),
                "creative_request",
                UUID.randomUUID()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldReserveCreditsAndReturnReservationResult() {
        UUID workspaceId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        when(creditWalletService.initializeWallet(workspaceId))
                .thenReturn(wallet(workspaceId, "5.0000", "0.0000"))
                .thenReturn(wallet(workspaceId, "5.0000", "1.0000"));
        when(creditWalletService.reserve(workspaceId, new BigDecimal("1.0000"), "creative_request", referenceId))
                .thenReturn(transaction(workspaceId, reservationId, CreditTransactionType.RESERVE, "1.0000", "creative_request", referenceId));

        CreditReservationResult result = service.reserveCredits(
                workspaceId,
                new BigDecimal("1.0000"),
                "creative_request",
                referenceId);

        assertThat(result.reservationId()).isEqualTo(reservationId);
        assertThat(result.reservedAmount()).isEqualByComparingTo("1.0000");
        assertThat(result.walletBalance()).isEqualByComparingTo("5.0000");
        assertThat(result.walletReservedBalance()).isEqualByComparingTo("1.0000");
        assertThat(result.walletAvailableBalance()).isEqualByComparingTo("4.0000");
    }

    @Test
    void shouldFinalizeOutstandingReservedCreditsFromReservationId() {
        UUID workspaceId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        when(creditTransactionRepository.findByIdAndWorkspaceIdAndDeletedFalse(reservationId, workspaceId))
                .thenReturn(java.util.Optional.of(
                        transaction(workspaceId, reservationId, CreditTransactionType.RESERVE, "3.0000", "creative_request", referenceId)));
        when(creditTransactionRepository.findAllByWorkspaceIdAndReferenceTypeAndReferenceIdAndDeletedFalseOrderByCreatedAtAsc(
                workspaceId,
                "creative_request",
                referenceId))
                .thenReturn(List.of(
                        transaction(workspaceId, reservationId, CreditTransactionType.RESERVE, "3.0000", "creative_request", referenceId),
                        transaction(workspaceId, UUID.randomUUID(), CreditTransactionType.REFUND, "1.0000", "creative_request", referenceId)));

        service.finalizeCredits(new CreditFinalizeCommand(
                workspaceId,
                reservationId,
                null,
                null,
                "generation_completed"));

        verify(creditWalletService).finalizeReservation(
                eq(workspaceId),
                eq(new BigDecimal("2.0000")),
                eq("creative_request"),
                eq(referenceId));
    }

    @Test
    void shouldSkipRefundWhenReservationAlreadySettled() {
        UUID workspaceId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();
        when(creditTransactionRepository.findAllByWorkspaceIdAndReferenceTypeAndReferenceIdAndDeletedFalseOrderByCreatedAtAsc(
                workspaceId,
                "creative_request",
                referenceId))
                .thenReturn(List.of(
                        transaction(workspaceId, UUID.randomUUID(), CreditTransactionType.RESERVE, "1.0000", "creative_request", referenceId),
                        transaction(workspaceId, UUID.randomUUID(), CreditTransactionType.FINALIZE, "1.0000", "creative_request", referenceId)));

        service.refundCredits(new CreditRefundCommand(
                workspaceId,
                null,
                "creative_request",
                referenceId,
                "generation_failed"));

        verify(creditWalletService, never()).refund(
                eq(workspaceId),
                eq(new BigDecimal("1.0000")),
                eq("creative_request"),
                eq(referenceId));
    }

    private CreditWalletEntity wallet(UUID workspaceId, String balance, String reservedBalance) {
        CreditWalletEntity wallet = CreditWalletEntity.initialize(workspaceId);
        wallet.addBalance(new BigDecimal(balance));
        wallet.reserve(new BigDecimal(reservedBalance));
        return wallet;
    }

    private CreditTransactionEntity transaction(
            UUID workspaceId,
            UUID transactionId,
            CreditTransactionType type,
            String amount,
            String referenceType,
            UUID referenceId
    ) {
        CreditTransactionEntity transaction = CreditTransactionEntity.create(
                workspaceId,
                type,
                new BigDecimal(amount),
                referenceType,
                referenceId,
                CreditTransactionStatus.COMPLETED);
        forceId(transaction, transactionId);
        return transaction;
    }

    private void forceId(CreditTransactionEntity transaction, UUID transactionId) {
        try {
            java.lang.reflect.Field field = transaction.getClass().getSuperclass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(transaction, transactionId);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
