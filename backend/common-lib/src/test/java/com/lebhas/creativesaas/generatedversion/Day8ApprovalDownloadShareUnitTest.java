package com.lebhas.creativesaas.generatedversion;

import com.lebhas.creativesaas.asset.application.AssetEventPublisher;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.creativerequest.application.CreativeRequestQueryService;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionReviewService;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionViewMapper;
import com.lebhas.creativesaas.generatedversion.application.dto.GeneratedVersionApprovalActionCommand;
import com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionApprovalHistory;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionApprovalHistoryRepository;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionRepository;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.sharing.application.SecureTokenService;
import com.lebhas.creativesaas.sharing.domain.ShareLink;
import com.lebhas.creativesaas.workspace.domain.WorkspaceEntity;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Day8ApprovalDownloadShareUnitTest {

    private static final UUID WORKSPACE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void shareTokenHashStoredWithoutRawToken() {
        SecureTokenService tokenService = new SecureTokenService(NoOpPasswordEncoder.getInstance());
        String rawToken = "raw-public-share-token";
        String tokenHash = tokenService.hashToken(rawToken);

        ShareLink shareLink = ShareLink.create(
                WORKSPACE_ID,
                UUID.randomUUID(),
                tokenHash,
                Instant.now().plusSeconds(3600),
                USER_ID);

        assertThat(tokenHash).isNotEqualTo(rawToken);
        assertThat(tokenService.hashToken(rawToken)).isEqualTo(tokenHash);
        assertThat(shareLink.getTokenHash()).isEqualTo(tokenHash);
        assertThat(shareLink.getTokenHash()).doesNotContain(rawToken);
    }

    @Test
    void revokedShareLinkIsMarkedRevoked() {
        ShareLink shareLink = ShareLink.create(
                WORKSPACE_ID,
                UUID.randomUUID(),
                "token-hash",
                Instant.now().plusSeconds(3600),
                USER_ID);

        shareLink.revoke(USER_ID);

        assertThat(shareLink.isRevoked()).isTrue();
        assertThat(shareLink.getRevokedAt()).isNotNull();
        assertThat(shareLink.getRevokedBy()).isEqualTo(USER_ID);
    }

    @Test
    void rejectAndRequestChangesRequireComment() {
        GeneratedVersionReviewService service = reviewService(
                mock(GeneratedVersionRepository.class),
                mock(GeneratedVersionApprovalHistoryRepository.class));

        assertThatThrownBy(() -> service.reject(new GeneratedVersionApprovalActionCommand(WORKSPACE_ID, UUID.randomUUID(), " ")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.requestChanges(new GeneratedVersionApprovalActionCommand(WORKSPACE_ID, UUID.randomUUID(), null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void approveUpdatesGeneratedVersionStatusAndHistory() {
        UUID generatedVersionId = UUID.randomUUID();
        GeneratedVersionEntity version = GeneratedVersionEntity.create(
                WORKSPACE_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                "Version 1",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "provider",
                "model",
                USER_ID);
        ReflectionTestUtils.setField(version, "id", generatedVersionId);

        GeneratedVersionRepository versionRepository = mock(GeneratedVersionRepository.class);
        GeneratedVersionApprovalHistoryRepository historyRepository = mock(GeneratedVersionApprovalHistoryRepository.class);
        when(versionRepository.findByIdAndWorkspaceIdAndDeletedFalse(generatedVersionId, WORKSPACE_ID)).thenReturn(Optional.of(version));
        when(versionRepository.save(any(GeneratedVersionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.save(any(GeneratedVersionApprovalHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = reviewService(versionRepository, historyRepository)
                .approve(new GeneratedVersionApprovalActionCommand(WORKSPACE_ID, generatedVersionId, "Approved"));

        assertThat(result.approvalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(version.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(version.getLatestApprovalComment()).isEqualTo("Approved");
    }

    private GeneratedVersionReviewService reviewService(
            GeneratedVersionRepository versionRepository,
            GeneratedVersionApprovalHistoryRepository historyRepository
    ) {
        WorkspaceAuthorizationService authorizationService = mock(WorkspaceAuthorizationService.class);
        CreativeRequestQueryService creativeRequestQueryService = mock(CreativeRequestQueryService.class);
        AssetEventPublisher eventPublisher = mock(AssetEventPublisher.class);
        WorkspaceAuthorizationService.WorkspaceAccess access = mock(WorkspaceAuthorizationService.WorkspaceAccess.class);
        WorkspaceEntity workspace = mock(WorkspaceEntity.class);
        when(workspace.getId()).thenReturn(WORKSPACE_ID);
        when(access.workspace()).thenReturn(workspace);
        when(access.currentUser()).thenReturn(currentUser());
        when(authorizationService.requirePermission(WORKSPACE_ID, Permission.GENERATED_VERSION_MANAGE)).thenReturn(access);
        return new GeneratedVersionReviewService(
                authorizationService,
                creativeRequestQueryService,
                versionRepository,
                historyRepository,
                new GeneratedVersionViewMapper(),
                eventPublisher);
    }

    private CurrentUser currentUser() {
        return new CurrentUser(
                USER_ID,
                WORKSPACE_ID,
                null,
                "reviewer@example.com",
                Set.of(Role.ADMIN),
                Set.of(Permission.GENERATED_VERSION_MANAGE),
                UUID.randomUUID().toString(),
                Instant.now().plusSeconds(3600));
    }
}
