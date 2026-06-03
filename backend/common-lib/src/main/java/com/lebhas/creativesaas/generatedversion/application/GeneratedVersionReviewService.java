package com.lebhas.creativesaas.generatedversion.application;

import com.lebhas.creativesaas.asset.application.AssetEventPublisher;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.creativerequest.application.CreativeRequestQueryService;
import com.lebhas.creativesaas.generatedversion.application.dto.GeneratedVersionApprovalActionCommand;
import com.lebhas.creativesaas.generatedversion.application.dto.GeneratedVersionApprovalHistoryView;
import com.lebhas.creativesaas.generatedversion.application.dto.GeneratedVersionView;
import com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionApprovalAction;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionApprovalHistory;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionApprovalHistoryRepository;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionRepository;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GeneratedVersionReviewService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final CreativeRequestQueryService creativeRequestQueryService;
    private final GeneratedVersionRepository generatedVersionRepository;
    private final GeneratedVersionApprovalHistoryRepository historyRepository;
    private final GeneratedVersionViewMapper generatedVersionViewMapper;
    private final AssetEventPublisher eventPublisher;

    public GeneratedVersionReviewService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            CreativeRequestQueryService creativeRequestQueryService,
            GeneratedVersionRepository generatedVersionRepository,
            GeneratedVersionApprovalHistoryRepository historyRepository,
            GeneratedVersionViewMapper generatedVersionViewMapper,
            AssetEventPublisher eventPublisher
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.creativeRequestQueryService = creativeRequestQueryService;
        this.generatedVersionRepository = generatedVersionRepository;
        this.historyRepository = historyRepository;
        this.generatedVersionViewMapper = generatedVersionViewMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<GeneratedVersionView> reviewQueue(UUID workspaceId) {
        WorkspaceAuthorizationService.WorkspaceAccess access =
                workspaceAuthorizationService.requirePermission(workspaceId, Permission.WORKSPACE_VIEW);
        return List.of(ApprovalStatus.SUBMITTED, ApprovalStatus.IN_REVIEW, ApprovalStatus.RESUBMITTED)
                .stream()
                .flatMap(status -> generatedVersionRepository
                        .findAllByWorkspaceIdAndApprovalStatusAndDeletedFalseOrderByUpdatedAtDesc(workspaceId, status)
                        .stream())
                .filter(version -> creativeRequestQueryService.requireAccessibleRequest(
                        workspaceId,
                        version.getCreativeRequestId(),
                        access) != null)
                .map(generatedVersionViewMapper::toView)
                .toList();
    }

    @Transactional
    public GeneratedVersionView approve(GeneratedVersionApprovalActionCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requireManager(command.workspaceId());
        GeneratedVersionEntity version = requireVersion(command.workspaceId(), command.generatedVersionId(), access);
        version.markApproved(access.currentUser().userId(), command.comment());
        GeneratedVersionEntity saved = generatedVersionRepository.save(version);
        record(saved, GeneratedVersionApprovalAction.APPROVE, access.currentUser().userId(), command.comment());
        publish(KafkaTopicConstants.GENERATED_VERSION_APPROVED, saved, access.currentUser().userId());
        return generatedVersionViewMapper.toView(saved);
    }

    @Transactional
    public GeneratedVersionView reject(GeneratedVersionApprovalActionCommand command) {
        requireComment(command.comment(), "Reject requires a comment");
        WorkspaceAuthorizationService.WorkspaceAccess access = requireManager(command.workspaceId());
        GeneratedVersionEntity version = requireVersion(command.workspaceId(), command.generatedVersionId(), access);
        version.markRejected(access.currentUser().userId(), command.comment(), true);
        GeneratedVersionEntity saved = generatedVersionRepository.save(version);
        record(saved, GeneratedVersionApprovalAction.REJECT, access.currentUser().userId(), command.comment());
        publish(KafkaTopicConstants.GENERATED_VERSION_REJECTED, saved, access.currentUser().userId());
        return generatedVersionViewMapper.toView(saved);
    }

    @Transactional
    public GeneratedVersionView requestChanges(GeneratedVersionApprovalActionCommand command) {
        requireComment(command.comment(), "Request changes requires a comment");
        WorkspaceAuthorizationService.WorkspaceAccess access = requireManager(command.workspaceId());
        GeneratedVersionEntity version = requireVersion(command.workspaceId(), command.generatedVersionId(), access);
        version.markChangesRequested(access.currentUser().userId(), command.comment());
        GeneratedVersionEntity saved = generatedVersionRepository.save(version);
        record(saved, GeneratedVersionApprovalAction.REQUEST_CHANGES, access.currentUser().userId(), command.comment());
        publish(KafkaTopicConstants.GENERATED_VERSION_CHANGES_REQUESTED, saved, access.currentUser().userId());
        return generatedVersionViewMapper.toView(saved);
    }

    @Transactional(readOnly = true)
    public List<GeneratedVersionApprovalHistoryView> history(UUID workspaceId, UUID generatedVersionId) {
        WorkspaceAuthorizationService.WorkspaceAccess access =
                workspaceAuthorizationService.requirePermission(workspaceId, Permission.WORKSPACE_VIEW);
        requireVersion(workspaceId, generatedVersionId, access);
        return historyRepository
                .findAllByWorkspaceIdAndGeneratedVersionIdAndDeletedFalseOrderByCreatedAtAsc(workspaceId, generatedVersionId)
                .stream()
                .map(this::toView)
                .toList();
    }

    private WorkspaceAuthorizationService.WorkspaceAccess requireManager(UUID workspaceId) {
        return workspaceAuthorizationService.requirePermission(workspaceId, Permission.GENERATED_VERSION_MANAGE);
    }

    private GeneratedVersionEntity requireVersion(
            UUID workspaceId,
            UUID generatedVersionId,
            WorkspaceAuthorizationService.WorkspaceAccess access
    ) {
        GeneratedVersionEntity version = generatedVersionRepository.findByIdAndWorkspaceIdAndDeletedFalse(generatedVersionId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GENERATED_VERSION_NOT_FOUND));
        creativeRequestQueryService.requireAccessibleRequest(workspaceId, version.getCreativeRequestId(), access);
        return version;
    }

    private void record(
            GeneratedVersionEntity version,
            GeneratedVersionApprovalAction action,
            UUID actorUserId,
            String comment
    ) {
        historyRepository.save(GeneratedVersionApprovalHistory.record(
                version.getWorkspaceId(),
                version.getId(),
                action,
                actorUserId,
                comment));
    }

    private GeneratedVersionApprovalHistoryView toView(GeneratedVersionApprovalHistory history) {
        return new GeneratedVersionApprovalHistoryView(
                history.getId(),
                history.getWorkspaceId(),
                history.getGeneratedVersionId(),
                history.getAction(),
                history.getActionBy(),
                history.getComment(),
                history.getCreatedAt());
    }

    private void publish(String topic, GeneratedVersionEntity version, UUID actorUserId) {
        eventPublisher.publish(topic, version.getWorkspaceId(), version.getId(), Map.of(
                "workspaceId", version.getWorkspaceId().toString(),
                "generatedVersionId", version.getId().toString(),
                "creativeRequestId", version.getCreativeRequestId().toString(),
                "actorUserId", actorUserId.toString()));
    }

    private void requireComment(String comment, String message) {
        if (!StringUtils.hasText(comment)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, message);
        }
    }
}
