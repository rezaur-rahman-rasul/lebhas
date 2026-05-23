package com.lebhas.creativesaas.generation.application;

import com.lebhas.creativesaas.generation.application.dto.CreditFinalizeCommand;
import com.lebhas.creativesaas.generation.application.dto.CreditRefundCommand;
import com.lebhas.creativesaas.generation.domain.CreativeGenerationRequestEntity;
import com.lebhas.creativesaas.generation.domain.CreativeOutputFormat;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreativeGenerationCreditServiceTest {

    private final CreditEstimationService creditEstimationService = mock(CreditEstimationService.class);
    private final CreativeCreditReservationService creativeCreditReservationService = mock(CreativeCreditReservationService.class);

    private CreativeGenerationCreditService service;

    @BeforeEach
    void setUp() {
        service = new CreativeGenerationCreditService(creditEstimationService, creativeCreditReservationService);
    }

    @Test
    void shouldDelegateBalanceValidationToReservationService() {
        UUID workspaceId = UUID.randomUUID();
        BigDecimal estimatedCost = new BigDecimal("1.0000");
        CreativeGenerationContext context = new CreativeGenerationContext(
                null,
                UUID.randomUUID(),
                "source prompt",
                null,
                PromptPlatform.FACEBOOK,
                CampaignObjective.SALES,
                CreativeType.STATIC_IMAGE,
                CreativeOutputFormat.PNG,
                PromptLanguage.ENGLISH,
                1024,
                1024,
                null,
                null,
                null,
                "{}",
                Map.of(),
                0);

        when(creditEstimationService.estimate(context)).thenReturn(estimatedCost);

        service.validateSufficientCredits(workspaceId, context);

        verify(creativeCreditReservationService).assertSufficientBalance(workspaceId, estimatedCost);
    }

    @Test
    void shouldReserveEstimatedCreditsForGenerationRequest() {
        CreativeGenerationRequestEntity request = request(CreativeType.STATIC_IMAGE);
        when(creditEstimationService.estimate(request)).thenReturn(new BigDecimal("1.0000"));

        service.reserveCredits(request);

        verify(creativeCreditReservationService).reserveCredits(
                eq(request.getWorkspaceId()),
                eq(new BigDecimal("1.0000")),
                eq("creative_generation_request"),
                eq(request.getId()));
    }

    @Test
    void shouldFinalizeCreditsUsingGenerationReference() {
        CreativeGenerationRequestEntity request = request(CreativeType.SHORT_VIDEO);

        service.commitCreditUsage(request);

        verify(creativeCreditReservationService).finalizeCredits(new CreditFinalizeCommand(
                request.getWorkspaceId(),
                null,
                "creative_generation_request",
                request.getId(),
                "generation_request_completed"));
    }

    @Test
    void shouldRefundCreditsUsingGenerationReference() {
        CreativeGenerationRequestEntity request = request(CreativeType.STATIC_IMAGE);

        service.releaseReservedCredits(request);

        verify(creativeCreditReservationService).refundCredits(new CreditRefundCommand(
                request.getWorkspaceId(),
                null,
                "creative_generation_request",
                request.getId(),
                "generation_request_failed"));
    }

    private CreativeGenerationRequestEntity request(CreativeType creativeType) {
        return CreativeGenerationRequestEntity.queue(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                "source prompt",
                null,
                PromptPlatform.FACEBOOK,
                CampaignObjective.SALES,
                creativeType,
                creativeType.isVideo() ? CreativeOutputFormat.MP4 : CreativeOutputFormat.PNG,
                PromptLanguage.ENGLISH,
                null,
                null,
                "{}",
                "MOCK",
                "mock-image-v1");
    }
}
