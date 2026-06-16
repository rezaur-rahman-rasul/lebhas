package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.credit.domain.CreditTransactionEntity;
import com.lebhas.creativesaas.credit.infrastructure.persistence.CreditTransactionRepository;
import com.lebhas.creativesaas.credit.domain.CreditWalletEntity;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generation.application.CreditEstimationService;
import com.lebhas.creativesaas.generation.event.GenerationEventProducer;
import com.lebhas.creativesaas.usage.domain.CreditLedger;
import com.lebhas.creativesaas.usage.domain.CreditLedgerTransactionType;
import com.lebhas.creativesaas.usage.domain.WorkspaceUsageSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreditUsageServicePromptRequestTest {

    private static final UUID WORKSPACE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PROMPT_REQUEST_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CREATIVE_GENERATION_JOB_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final BigDecimal ONE_CREDIT = new BigDecimal("1.0000");

    @Test
    void promptRequestReservationDoesNotWriteCreativeGenerationJobIdToLegacyLedgerFk() {
        CreditBalanceService creditBalanceService = mock(CreditBalanceService.class);
        CreditLedgerService creditLedgerService = mock(CreditLedgerService.class);
        CreditTransactionRepository transactionRepository = mock(CreditTransactionRepository.class);
        WorkspaceUsageSummaryService summaryService = mock(WorkspaceUsageSummaryService.class);
        WorkspaceUsageSummary summary = WorkspaceUsageSummary.create(WORKSPACE_ID, LocalDate.of(2026, 6, 1));
        CreditWalletEntity wallet = CreditWalletEntity.initialize(WORKSPACE_ID);
        wallet.addBalance(new BigDecimal("10"));
        wallet.reserve(BigDecimal.ONE);
        CreditLedger ledger = CreditLedger.create(
                WORKSPACE_ID,
                null,
                null,
                null,
                CreditLedgerTransactionType.RESERVE,
                ONE_CREDIT,
                new BigDecimal("10"),
                new BigDecimal("10"),
                "AI_CREATIVE_PROMPT_REQUEST",
                PROMPT_REQUEST_ID,
                "Credit reservation for direct AI creative generation",
                USER_ID);
        ReflectionTestUtils.setField(ledger, "id", UUID.randomUUID());

        when(creditBalanceService.withCreditLock(eq(WORKSPACE_ID), any())).thenAnswer(invocation -> {
            Supplier<?> action = invocation.getArgument(1);
            return action.get();
        });
        when(creditBalanceService.reserve(WORKSPACE_ID, ONE_CREDIT))
                .thenReturn(new CreditBalanceService.BalanceMovement(wallet, new BigDecimal("10"), new BigDecimal("10")));
        when(transactionRepository.save(any(CreditTransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(creditLedgerService.append(
                eq(WORKSPACE_ID),
                eq(null),
                eq(null),
                eq(null),
                eq(CreditLedgerTransactionType.RESERVE),
                eq(ONE_CREDIT),
                eq(new BigDecimal("10")),
                eq(new BigDecimal("10")),
                eq("AI_CREATIVE_PROMPT_REQUEST"),
                eq(PROMPT_REQUEST_ID),
                eq("Credit reservation for direct AI creative generation"),
                eq(USER_ID))).thenReturn(ledger);
        when(summaryService.getOrCreateSummary(eq(WORKSPACE_ID), any(LocalDate.class))).thenReturn(summary);

        CreditUsageService service = new CreditUsageService(
                creditBalanceService,
                creditLedgerService,
                new CreditUsageMapper(),
                transactionRepository,
                mock(CreativeRequestRepository.class),
                summaryService,
                mock(CreditEstimationService.class),
                mockGenerationEventProducerProvider());

        var result = service.reservePromptRequestCredits(
                WORKSPACE_ID,
                PROMPT_REQUEST_ID,
                CREATIVE_GENERATION_JOB_ID,
                BigDecimal.ONE,
                USER_ID);

        assertThat(result.referenceType()).isEqualTo("AI_CREATIVE_PROMPT_REQUEST");
        assertThat(result.referenceId()).isEqualTo(PROMPT_REQUEST_ID);
        assertThat(ledger.getGenerationJobId()).isNull();
        verify(creditLedgerService).append(
                eq(WORKSPACE_ID),
                eq(null),
                eq(null),
                eq(null),
                eq(CreditLedgerTransactionType.RESERVE),
                eq(ONE_CREDIT),
                eq(new BigDecimal("10")),
                eq(new BigDecimal("10")),
                eq("AI_CREATIVE_PROMPT_REQUEST"),
                eq(PROMPT_REQUEST_ID),
                eq("Credit reservation for direct AI creative generation"),
                eq(USER_ID));
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<GenerationEventProducer> mockGenerationEventProducerProvider() {
        ObjectProvider<GenerationEventProducer> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }
}
