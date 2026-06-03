package com.lebhas.creativesaas.approval;

import com.lebhas.creativesaas.approval.application.ApprovalActionService;
import com.lebhas.creativesaas.approval.application.ApprovalHistoryService;
import com.lebhas.creativesaas.approval.application.ApprovalMapper;
import com.lebhas.creativesaas.approval.application.ApprovalWorkflowService;
import com.lebhas.creativesaas.approval.application.dto.ApprovalActionCommand;
import com.lebhas.creativesaas.approval.application.dto.ApprovalWorkflowView;
import com.lebhas.creativesaas.approval.application.dto.CreateApprovalWorkflowCommand;
import com.lebhas.creativesaas.approval.cache.ApprovalRedisAccessSupport;
import com.lebhas.creativesaas.approval.cache.ApprovalRedisCacheProperties;
import com.lebhas.creativesaas.approval.cache.ApprovalRedisKeys;
import com.lebhas.creativesaas.approval.cache.ApprovalRedisTtlStrategy;
import com.lebhas.creativesaas.approval.cache.ApprovalStateCacheService;
import com.lebhas.creativesaas.approval.cache.ApprovalWorkflowCacheEntry;
import com.lebhas.creativesaas.approval.cache.ApprovalWorkflowCacheService;
import com.lebhas.creativesaas.approval.cache.ReviewerAssignmentCacheService;
import com.lebhas.creativesaas.approval.domain.ApprovalAction;
import com.lebhas.creativesaas.approval.domain.ApprovalHistory;
import com.lebhas.creativesaas.approval.domain.ApprovalStatus;
import com.lebhas.creativesaas.approval.domain.ApprovalWorkflow;
import com.lebhas.creativesaas.approval.event.ApprovalEventProducer;
import com.lebhas.creativesaas.approval.event.ApprovalWorkflowEvent;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalHistoryRepository;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalWorkflowRepository;
import com.lebhas.creativesaas.approval.validation.ApprovalPermissionValidationService;
import com.lebhas.creativesaas.approval.validation.ApprovalPermissionValidator;
import com.lebhas.creativesaas.approval.validation.ApprovalPlanValidationService;
import com.lebhas.creativesaas.approval.validation.ApprovalWorkflowValidationService;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.TenantIsolationException;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionStatus;
import com.lebhas.creativesaas.generatedversion.domain.GenerationStatus;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionRepository;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.pricing.application.WorkspacePlanContextService;
import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.PricingPlanView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspacePlanContextView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspaceSubscriptionView;
import com.lebhas.creativesaas.redis.RedisCacheService;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.sharing.application.SecureTokenService;
import com.lebhas.creativesaas.sharing.application.ShareLinkMapper;
import com.lebhas.creativesaas.sharing.application.ShareLinkService;
import com.lebhas.creativesaas.sharing.application.dto.CreateRevisedShareLinkCommand;
import com.lebhas.creativesaas.sharing.application.dto.RevisedShareLinkView;
import com.lebhas.creativesaas.sharing.cache.ShareLinkCacheService;
import com.lebhas.creativesaas.sharing.domain.ShareLink;
import com.lebhas.creativesaas.sharing.event.ShareLinkCreatedEvent;
import com.lebhas.creativesaas.sharing.event.ShareLinkEventProducer;
import com.lebhas.creativesaas.sharing.infrastructure.persistence.ShareLinkRepository;
import com.lebhas.creativesaas.sharing.validation.ShareLinkValidationService;
import com.lebhas.creativesaas.workspace.domain.WorkspaceEntity;
import com.lebhas.creativesaas.workspace.domain.WorkspaceLanguage;
import com.lebhas.pricing.WorkspaceSubscriptionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RevisedDay6ApprovalShareUnitTest {

    private static final Instant NOW = Instant.parse("2026-05-21T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID WORKSPACE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID REVIEWER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID CREATIVE_REQUEST_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID GENERATED_VERSION_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Test
    void approvalWorkflowBlockedIfPlanDisallowsFeature() {
        WorkspacePlanContextService planContextService = mock(WorkspacePlanContextService.class);
        when(planContextService.getWorkspacePlanContext(WORKSPACE_ID)).thenReturn(planContext(false, true));
        ApprovalPlanValidationService service = new ApprovalPlanValidationService(planContextService);

        assertThatThrownBy(() -> service.requireApprovalWorkflowEnabled(WORKSPACE_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Approval workflow is not enabled");
    }

    @Test
    void approvalWorkflowCreatedCorrectly() {
        ApprovalWorkflowRepository workflowRepository = mock(ApprovalWorkflowRepository.class);
        GeneratedVersionRepository generatedVersionRepository = mock(GeneratedVersionRepository.class);
        ApprovalWorkflowValidationService workflowValidationService = mock(ApprovalWorkflowValidationService.class);
        ApprovalPermissionValidationService permissionValidationService = mock(ApprovalPermissionValidationService.class);
        ApprovalWorkflowCacheService workflowCacheService = mock(ApprovalWorkflowCacheService.class);
        ApprovalStateCacheService stateCacheService = mock(ApprovalStateCacheService.class);
        ReviewerAssignmentCacheService assignmentCacheService = mock(ReviewerAssignmentCacheService.class);
        ApprovalEventProducer eventProducer = mock(ApprovalEventProducer.class);
        ApprovalWorkflowService service = new ApprovalWorkflowService(
                workflowRepository,
                generatedVersionRepository,
                workflowValidationService,
                permissionValidationService,
                workflowCacheService,
                stateCacheService,
                assignmentCacheService,
                new ApprovalMapper(),
                eventProducer,
                CLOCK);
        when(permissionValidationService.requireApprovalCreation(WORKSPACE_ID)).thenReturn(workspaceAccess(Role.CREW, Permission.CREATIVE_SUBMIT));
        when(workflowRepository.findByWorkspaceIdAndGeneratedVersionId(WORKSPACE_ID, GENERATED_VERSION_ID)).thenReturn(Optional.empty());
        when(workflowRepository.save(any(ApprovalWorkflow.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));
        when(generatedVersionRepository.findByIdAndWorkspaceIdAndDeletedFalse(GENERATED_VERSION_ID, WORKSPACE_ID))
                .thenReturn(Optional.of(generatedVersion()));

        ApprovalWorkflowView view = service.createApprovalWorkflow(new CreateApprovalWorkflowCommand(
                WORKSPACE_ID,
                CREATIVE_REQUEST_ID,
                GENERATED_VERSION_ID,
                REVIEWER_ID));

        assertThat(view.workspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(view.creativeRequestId()).isEqualTo(CREATIVE_REQUEST_ID);
        assertThat(view.generatedVersionId()).isEqualTo(GENERATED_VERSION_ID);
        assertThat(view.currentStatus()).isEqualTo(ApprovalStatus.PENDING);
        verify(eventProducer).publishRequested(any(ApprovalWorkflowEvent.class));
        verify(workflowCacheService).cacheWorkflow(any(ApprovalWorkflow.class));
        verify(stateCacheService).cacheState(any(ApprovalWorkflow.class));
        verify(assignmentCacheService).cacheAssignment(any(ApprovalWorkflow.class));
    }

    @Test
    void approvalStatusUpdatesCorrectly() {
        ApprovalWorkflow workflow = workflow();
        ApprovalActionService service = approvalActionService(workflow);

        ApprovalWorkflowView view = service.approve(new ApprovalActionCommand(WORKSPACE_ID, workflow.getId(), "approved"));

        assertThat(view.currentStatus()).isEqualTo(ApprovalStatus.APPROVED);
    }

    @Test
    void approvalHistoryStoresActions() {
        ApprovalHistoryRepository historyRepository = mock(ApprovalHistoryRepository.class);
        when(historyRepository.save(any(ApprovalHistory.class))).thenAnswer(invocation -> {
            ApprovalHistory history = invocation.getArgument(0);
            ReflectionTestUtils.setField(history, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(history, "createdAt", NOW);
            return history;
        });
        ApprovalHistoryService service = new ApprovalHistoryService(
                historyRepository,
                mock(ApprovalWorkflowValidationService.class),
                new ApprovalMapper());

        ApprovalHistory history = service.record(UUID.randomUUID(), USER_ID, ApprovalAction.REQUEST_REVISION, "revise");

        assertThat(history.getActionBy()).isEqualTo(USER_ID);
        assertThat(history.getActionType()).isEqualTo(ApprovalAction.REQUEST_REVISION);
        assertThat(history.getComments()).isEqualTo("revise");
    }

    @Test
    void shareLinkCreatedCorrectly() {
        ShareLinkRepository shareLinkRepository = mock(ShareLinkRepository.class);
        ShareLinkValidationService validationService = mock(ShareLinkValidationService.class);
        ShareLinkCacheService cacheService = mock(ShareLinkCacheService.class);
        ShareLinkEventProducer eventProducer = mock(ShareLinkEventProducer.class);
        WorkspaceAuthorizationService authorizationService = mock(WorkspaceAuthorizationService.class);
        ShareLinkService service = shareLinkService(authorizationService, shareLinkRepository, validationService, cacheService, eventProducer);
        when(authorizationService.requireWorkspaceContext(WORKSPACE_ID)).thenReturn(workspaceAccess(Role.CREW, Permission.CREATIVE_DOWNLOAD));
        when(shareLinkRepository.save(any(ShareLink.class))).thenAnswer(invocation -> persistedShareLink(invocation.getArgument(0)));

        RevisedShareLinkView view = service.createRevisedShareLink(new CreateRevisedShareLinkCommand(
                WORKSPACE_ID,
                GENERATED_VERSION_ID,
                "share-token",
                NOW.plus(Duration.ofDays(7))));

        assertThat(view.workspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(view.generatedVersionId()).isEqualTo(GENERATED_VERSION_ID);
        assertThat(view.token()).isEqualTo("share-token");
        assertThat(view.accessCount()).isZero();
        verify(validationService).requireShareLinkCreationAllowed(any(), eq(GENERATED_VERSION_ID), eq("share-token"));
        verify(cacheService).cacheShareLink(any(ShareLink.class));
        verify(eventProducer).publishCreated(any(ShareLinkCreatedEvent.class));
    }

    @Test
    void redisApprovalCacheWorks() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        ApprovalRedisAccessSupport accessSupport = new ApprovalRedisAccessSupport(redisCacheService, mock(RedisLockService.class));
        ApprovalWorkflowCacheService service = new ApprovalWorkflowCacheService(
                new ApprovalRedisKeys(),
                accessSupport,
                new ApprovalRedisTtlStrategy(new ApprovalRedisCacheProperties(), CLOCK));
        ApprovalWorkflow workflow = workflow();
        ApprovalWorkflowCacheEntry entry = ApprovalWorkflowCacheEntry.from(workflow);
        when(redisCacheService.get("approval:workflow:" + workflow.getId(), ApprovalWorkflowCacheEntry.class))
                .thenReturn(Optional.of(entry));

        assertThat(service.cacheWorkflow(workflow)).isTrue();
        assertThat(service.getWorkflow(workflow.getId())).contains(entry);
        verify(redisCacheService).set(eq("approval:workflow:" + workflow.getId()), any(ApprovalWorkflowCacheEntry.class), eq(Duration.ofHours(2)));
    }

    @Test
    void kafkaApprovalApprovedEventPublished() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        ApprovalEventProducer producer = new ApprovalEventProducer(kafkaTemplate);
        ApprovalWorkflowEvent event = approvalEvent(ApprovalStatus.APPROVED);

        producer.publishApproved(event);

        verify(kafkaTemplate).send(KafkaTopicConstants.APPROVAL_APPROVED, event.workflowId().toString(), event);
    }

    @Test
    void kafkaApprovalRejectedEventPublished() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        ApprovalEventProducer producer = new ApprovalEventProducer(kafkaTemplate);
        ApprovalWorkflowEvent event = approvalEvent(ApprovalStatus.REJECTED);

        producer.publishRejected(event);

        verify(kafkaTemplate).send(KafkaTopicConstants.APPROVAL_REJECTED, event.workflowId().toString(), event);
    }

    @Test
    void permissionValidationWorks() {
        WorkspaceAuthorizationService authorizationService = mock(WorkspaceAuthorizationService.class);
        ApprovalPermissionValidationService service = new ApprovalPermissionValidationService(
                authorizationService,
                mock(ApprovalPermissionValidator.class));
        when(authorizationService.requireWorkspaceContext(WORKSPACE_ID)).thenReturn(workspaceAccess(Role.CREW, Permission.CREATIVE_DOWNLOAD));

        assertThatThrownBy(() -> service.requireApprovalCreation(WORKSPACE_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Approval workflow creation is not permitted");
    }

    @Test
    void workspaceIsolationWorks() {
        ApprovalWorkflowValidationService service = new ApprovalWorkflowValidationService(
                mock(GeneratedVersionRepository.class),
                mock(com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository.class),
                mock(ApprovalWorkflowRepository.class),
                mock(ApprovalPlanValidationService.class));
        GeneratedVersionEntity version = generatedVersion();

        assertThatThrownBy(() -> service.requireGeneratedVersionMatchesCreativeRequest(version, UUID.randomUUID()))
                .isInstanceOf(TenantIsolationException.class);
    }

    @Test
    void approvalWorkflowLinkedToGeneratedVersion() {
        ApprovalWorkflow workflow = ApprovalWorkflow.create(WORKSPACE_ID, CREATIVE_REQUEST_ID, GENERATED_VERSION_ID, USER_ID, REVIEWER_ID);

        assertThat(workflow.getGeneratedVersionId()).isEqualTo(GENERATED_VERSION_ID);
    }

    @Test
    void shareTokenUniquenessValidated() {
        ShareLinkRepository shareLinkRepository = mock(ShareLinkRepository.class);
        when(shareLinkRepository.existsByToken("duplicate-token")).thenReturn(true);
        ShareLinkValidationService service = new ShareLinkValidationService(
                mock(GeneratedVersionRepository.class),
                shareLinkRepository,
                mock(WorkspacePlanContextService.class),
                mock(ApprovalPermissionValidationService.class));

        assertThatThrownBy(() -> service.requireTokenAvailable("duplicate-token"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Share token is already in use");
    }

    private ApprovalActionService approvalActionService(ApprovalWorkflow workflow) {
        ApprovalWorkflowService workflowService = mock(ApprovalWorkflowService.class);
        ApprovalHistoryService historyService = mock(ApprovalHistoryService.class);
        ApprovalWorkflowRepository workflowRepository = mock(ApprovalWorkflowRepository.class);
        ApprovalPermissionValidationService permissionValidationService = mock(ApprovalPermissionValidationService.class);
        when(permissionValidationService.requireApprovalVisibility(WORKSPACE_ID)).thenReturn(workspaceAccess(Role.ADMIN));
        when(workflowService.requireWorkflow(WORKSPACE_ID, workflow.getId())).thenReturn(workflow);
        when(workflowRepository.save(workflow)).thenReturn(workflow);
        return new ApprovalActionService(
                workflowService,
                historyService,
                workflowRepository,
                permissionValidationService,
                new ApprovalMapper());
    }

    private ShareLinkService shareLinkService(
            WorkspaceAuthorizationService authorizationService,
            ShareLinkRepository shareLinkRepository,
            ShareLinkValidationService validationService,
            ShareLinkCacheService cacheService,
            ShareLinkEventProducer eventProducer
    ) {
        SecureTokenService secureTokenService = mock(SecureTokenService.class);
        when(secureTokenService.hashToken(any())).thenAnswer(invocation -> invocation.getArgument(0));
        return new ShareLinkService(
                authorizationService,
                mock(GeneratedVersionRepository.class),
                secureTokenService,
                shareLinkRepository,
                validationService,
                mock(ApprovalPermissionValidationService.class),
                cacheService,
                new ShareLinkMapper(),
                eventProducer,
                null,
                CLOCK);
    }

    private ApprovalWorkflow workflow() {
        ApprovalWorkflow workflow = ApprovalWorkflow.create(WORKSPACE_ID, CREATIVE_REQUEST_ID, GENERATED_VERSION_ID, USER_ID, REVIEWER_ID);
        return persisted(workflow);
    }

    private ApprovalWorkflow persisted(ApprovalWorkflow workflow) {
        if (workflow.getId() == null) {
            ReflectionTestUtils.setField(workflow, "id", UUID.randomUUID());
        }
        ReflectionTestUtils.setField(workflow, "createdAt", NOW);
        ReflectionTestUtils.setField(workflow, "updatedAt", NOW);
        return workflow;
    }

    private ShareLink persistedShareLink(ShareLink shareLink) {
        ReflectionTestUtils.setField(shareLink, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(shareLink, "createdAt", NOW);
        return shareLink;
    }

    private ApprovalWorkflowEvent approvalEvent(ApprovalStatus status) {
        return new ApprovalWorkflowEvent(
                UUID.randomUUID(),
                WORKSPACE_ID,
                CREATIVE_REQUEST_ID,
                GENERATED_VERSION_ID,
                USER_ID,
                REVIEWER_ID,
                status,
                "comments",
                NOW);
    }

    private GeneratedVersionEntity generatedVersion() {
        GeneratedVersionEntity version = GeneratedVersionEntity.create(
                WORKSPACE_ID,
                CREATIVE_REQUEST_ID,
                UUID.randomUUID(),
                1,
                "Version 1",
                null,
                null,
                GenerationStatus.COMPLETED,
                com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus.NOT_SUBMITTED,
                true,
                "mock-provider",
                "mock-model",
                USER_ID,
                GeneratedVersionStatus.ACTIVE);
        ReflectionTestUtils.setField(version, "id", GENERATED_VERSION_ID);
        ReflectionTestUtils.setField(version, "createdAt", NOW);
        ReflectionTestUtils.setField(version, "updatedAt", NOW);
        return version;
    }

    private WorkspaceAuthorizationService.WorkspaceAccess workspaceAccess(Role role, Permission... permissions) {
        WorkspaceEntity workspace = WorkspaceEntity.create(
                "Workspace",
                "workspace",
                null,
                null,
                null,
                "UTC",
                WorkspaceLanguage.ENGLISH,
                "USD",
                "US",
                USER_ID);
        ReflectionTestUtils.setField(workspace, "id", WORKSPACE_ID);
        CurrentUser currentUser = new CurrentUser(
                USER_ID,
                WORKSPACE_ID,
                "device",
                "user@example.com",
                Set.of(role),
                Set.of(permissions),
                "token",
                NOW.plus(Duration.ofHours(1)));
        return new WorkspaceAuthorizationService.WorkspaceAccess(workspace, currentUser, null, role, Set.of(permissions));
    }

    private WorkspacePlanContextView planContext(boolean allowApprovalWorkflow, boolean allowPublicShareLinks) {
        UUID pricingPlanId = UUID.randomUUID();
        return new WorkspacePlanContextView(
                WORKSPACE_ID,
                new WorkspaceSubscriptionView(
                        UUID.randomUUID(),
                        WORKSPACE_ID,
                        pricingPlanId,
                        WorkspaceSubscriptionStatus.ACTIVE,
                        NOW.minus(Duration.ofDays(1)),
                        NOW.plus(Duration.ofDays(30)),
                        null,
                        true,
                        NOW,
                        NOW),
                new PricingPlanView(
                        pricingPlanId,
                        "Seed Example",
                        "SEED_EXAMPLE",
                        "Seed data only",
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "USD",
                        false,
                        true,
                        1,
                        NOW,
                        NOW),
                new PlanFeaturePolicyView(
                        UUID.randomUUID(),
                        pricingPlanId,
                        5,
                        5,
                        5,
                        5,
                        5,
                        BigDecimal.TEN,
                        BigDecimal.TEN,
                        allowApprovalWorkflow,
                        allowPublicShareLinks,
                        false,
                        false,
                        true,
                        true,
                        NOW,
                        NOW),
                false);
    }
}
