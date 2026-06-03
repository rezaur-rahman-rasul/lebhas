package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.creativerequest.application.dto.CreateCreativeRequestCommand;
import com.lebhas.creativesaas.creativerequest.application.dto.CreativeRequestResponse;
import com.lebhas.creativesaas.creativerequest.application.dto.CreativeRequestView;
import com.lebhas.creativesaas.creativerequest.application.dto.GenerationPreviewView;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestStatus;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generation.application.GenerationOrchestrator;
import com.lebhas.creativesaas.generation.cache.GenerationLockService;
import com.lebhas.creativesaas.generation.domain.CreativeOutputFormat;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.prompt.infrastructure.persistence.PromptDraftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreativeRequestBuilderServiceTest {

    @Mock
    private WorkspaceAuthorizationService workspaceAuthorizationService;
    @Mock
    private CreativeRequestValidationService creativeRequestValidationService;
    @Mock
    private CreativeRequestQueryService creativeRequestQueryService;
    @Mock
    private CreativeRequestRepository creativeRequestRepository;
    @Mock
    private com.lebhas.creativesaas.creativerequest.cache.CreativeRequestCacheService creativeRequestCacheService;
    @Mock
    private PromptDraftRepository promptDraftRepository;
    @Mock
    private GenerationOrchestrator generationOrchestrator;
    @Mock
    private GenerationLockService generationLockService;
    @Mock
    private CreativeRequestMapper creativeRequestMapper;
    @Mock
    private DomainEventPublisher domainEventPublisher;

    private CreativeRequestBuilderService service;
    private UUID workspaceId;
    private UUID userId;
    private UUID projectId;
    private WorkspaceAuthorizationService.WorkspaceAccess access;

    @BeforeEach
    void setUp() {
        service = new CreativeRequestBuilderService(
                workspaceAuthorizationService,
                creativeRequestValidationService,
                creativeRequestQueryService,
                creativeRequestRepository,
                creativeRequestCacheService,
                promptDraftRepository,
                generationOrchestrator,
                generationLockService,
                creativeRequestMapper,
                domainEventPublisher);
        workspaceId = UUID.randomUUID();
        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(
                userId,
                workspaceId,
                null,
                "owner@lebhas.test",
                Set.of(Role.ADMIN),
                Set.of(Permission.CREATIVE_REQUEST_CREATE, Permission.WORKSPACE_VIEW),
                "token",
                Instant.now().plusSeconds(3600));
        access = new WorkspaceAuthorizationService.WorkspaceAccess(null, currentUser, null, Role.ADMIN, currentUser.permissions());
    }

    @Test
    void manualCreativeRequestCreatesDraftWithoutQueueingGeneration() {
        CreateCreativeRequestCommand command = command();
        CreativeRequestGenerationPlan plan = plan();
        when(workspaceAuthorizationService.requirePermission(workspaceId, Permission.CREATIVE_REQUEST_CREATE)).thenReturn(access);
        when(creativeRequestValidationService.validateForCreate(command, access)).thenReturn(plan);
        when(creativeRequestRepository.save(any(CreativeRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(creativeRequestMapper.toResponse(any(CreativeRequestEntity.class), any(), any(), any()))
                .thenReturn(response(CreativeRequestStatus.DRAFT));

        CreativeRequestResponse response = service.createManual(command);

        assertThat(response.request().status()).isEqualTo(CreativeRequestStatus.DRAFT);
        ArgumentCaptor<CreativeRequestEntity> savedRequest = ArgumentCaptor.forClass(CreativeRequestEntity.class);
        verify(creativeRequestRepository).save(savedRequest.capture());
        assertThat(savedRequest.getValue().getStatus()).isEqualTo(CreativeRequestStatus.DRAFT);
        verify(generationOrchestrator, never()).queueGeneration(any(), any(), any());
    }

    @Test
    void generationPreviewReturnsEstimateWithoutQueueingOrReservingCredits() {
        UUID creativeRequestId = UUID.randomUUID();
        CreativeRequestEntity request = request(creativeRequestId, CreativeRequestStatus.DRAFT);
        when(workspaceAuthorizationService.requirePermission(workspaceId, Permission.CREATIVE_REQUEST_CREATE)).thenReturn(access);
        when(creativeRequestQueryService.requireAccessibleRequest(workspaceId, creativeRequestId, access)).thenReturn(request);
        when(creativeRequestValidationService.validateForRetry(request, access)).thenReturn(plan());

        GenerationPreviewView preview = service.preview(workspaceId, creativeRequestId);

        assertThat(preview.creativeRequestId()).isEqualTo(creativeRequestId);
        assertThat(preview.estimatedCreditCost()).isEqualByComparingTo("2.0000");
        assertThat(preview.creditsReserved()).isFalse();
        verify(generationOrchestrator, never()).queueGeneration(any(), any(), any());
    }

    @Test
    void duplicateQueueIsBlockedBeforeGenerationJobCreation() {
        UUID creativeRequestId = UUID.randomUUID();
        CreativeRequestEntity request = request(creativeRequestId, CreativeRequestStatus.DRAFT);
        when(workspaceAuthorizationService.requirePermission(workspaceId, Permission.CREATIVE_REQUEST_CREATE)).thenReturn(access);
        when(creativeRequestQueryService.requireAccessibleRequest(workspaceId, creativeRequestId, access)).thenReturn(request);
        when(creativeRequestValidationService.validateForRetry(request, access)).thenReturn(plan());
        when(generationLockService.acquire(workspaceId, creativeRequestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.queue(workspaceId, creativeRequestId))
                .hasMessageContaining("Generation is already queued");
        verify(generationOrchestrator, never()).queueGeneration(any(), any(), any());
    }

    private CreateCreativeRequestCommand command() {
        return new CreateCreativeRequestCommand(
                workspaceId,
                null,
                null,
                projectId,
                "Eid campaign",
                "Create an Eid ad",
                null,
                BrandLanguagePreference.ENGLISH,
                CampaignObjective.SALES.name(),
                PromptPlatform.FACEBOOK.name(),
                CreativeOutputFormat.PNG.name(),
                2,
                List.of());
    }

    private CreativeRequestGenerationPlan plan() {
        return new CreativeRequestGenerationPlan(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                projectId,
                "Eid campaign",
                "Create an Eid ad",
                null,
                BrandLanguagePreference.ENGLISH,
                null,
                null,
                null,
                CampaignObjective.SALES.name(),
                PromptPlatform.FACEBOOK.name(),
                CreativeOutputFormat.PNG.name(),
                2,
                List.of(),
                PromptPlatform.FACEBOOK,
                CampaignObjective.SALES,
                CreativeOutputFormat.PNG,
                CreativeType.STATIC_IMAGE,
                PromptLanguage.ENGLISH,
                new BigDecimal("2.0000"),
                "duplicate-hash");
    }

    private CreativeRequestEntity request(UUID creativeRequestId, CreativeRequestStatus status) {
        CreativeRequestEntity request = org.mockito.Mockito.mock(CreativeRequestEntity.class);
        lenient().when(request.getId()).thenReturn(creativeRequestId);
        lenient().when(request.getWorkspaceId()).thenReturn(workspaceId);
        lenient().when(request.getProjectCampaignId()).thenReturn(projectId);
        when(request.getStatus()).thenReturn(status);
        lenient().when(request.getRequestedVersions()).thenReturn(2);
        return request;
    }

    private CreativeRequestResponse response(CreativeRequestStatus status) {
        return new CreativeRequestResponse(
                new CreativeRequestView(
                        UUID.randomUUID(),
                        workspaceId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        projectId,
                        userId,
                        null,
                        "Eid campaign",
                        "Create an Eid ad",
                        null,
                        BrandLanguagePreference.ENGLISH,
                        CreativeType.STATIC_IMAGE.name(),
                        CampaignObjective.SALES.name(),
                        PromptPlatform.FACEBOOK.name(),
                        null,
                        null,
                        null,
                        2,
                        0,
                        null,
                        null,
                        null,
                        CreativeOutputFormat.PNG.name(),
                        List.of(),
                        status,
                        null,
                        Instant.now(),
                        Instant.now()),
                null,
                List.of(),
                null,
                new BigDecimal("2.0000"));
    }
}
