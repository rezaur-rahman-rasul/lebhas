package com.lebhas.creativesaas.approval.application;

import com.lebhas.creativesaas.approval.application.dto.ApprovalWorkflowView;
import com.lebhas.creativesaas.approval.application.dto.CreateApprovalWorkflowCommand;
import com.lebhas.creativesaas.approval.cache.ApprovalStateCacheService;
import com.lebhas.creativesaas.approval.cache.ApprovalWorkflowCacheEntry;
import com.lebhas.creativesaas.approval.cache.ApprovalWorkflowCacheService;
import com.lebhas.creativesaas.approval.cache.ReviewerAssignmentCacheService;
import com.lebhas.creativesaas.approval.domain.ApprovalStatus;
import com.lebhas.creativesaas.approval.domain.ApprovalWorkflow;
import com.lebhas.creativesaas.approval.event.ApprovalEventProducer;
import com.lebhas.creativesaas.approval.event.ApprovalWorkflowEvent;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalWorkflowRepository;
import com.lebhas.creativesaas.approval.validation.ApprovalPermissionValidationService;
import com.lebhas.creativesaas.approval.validation.ApprovalWorkflowValidationService;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionRepository;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class ApprovalWorkflowService {

    private final ApprovalWorkflowRepository approvalWorkflowRepository;
    private final GeneratedVersionRepository generatedVersionRepository;
    private final ApprovalWorkflowValidationService approvalWorkflowValidationService;
    private final ApprovalPermissionValidationService approvalPermissionValidationService;
    private final ApprovalWorkflowCacheService approvalWorkflowCacheService;
    private final ApprovalStateCacheService approvalStateCacheService;
    private final ReviewerAssignmentCacheService reviewerAssignmentCacheService;
    private final ApprovalMapper approvalMapper;
    private final ApprovalEventProducer approvalEventProducer;
    private final Clock clock;

    public ApprovalWorkflowService(
            ApprovalWorkflowRepository approvalWorkflowRepository,
            GeneratedVersionRepository generatedVersionRepository,
            ApprovalWorkflowValidationService approvalWorkflowValidationService,
            ApprovalPermissionValidationService approvalPermissionValidationService,
            ApprovalWorkflowCacheService approvalWorkflowCacheService,
            ApprovalStateCacheService approvalStateCacheService,
            ReviewerAssignmentCacheService reviewerAssignmentCacheService,
            ApprovalMapper approvalMapper,
            ApprovalEventProducer approvalEventProducer,
            Clock clock
    ) {
        this.approvalWorkflowRepository = approvalWorkflowRepository;
        this.generatedVersionRepository = generatedVersionRepository;
        this.approvalWorkflowValidationService = approvalWorkflowValidationService;
        this.approvalPermissionValidationService = approvalPermissionValidationService;
        this.approvalWorkflowCacheService = approvalWorkflowCacheService;
        this.approvalStateCacheService = approvalStateCacheService;
        this.reviewerAssignmentCacheService = reviewerAssignmentCacheService;
        this.approvalMapper = approvalMapper;
        this.approvalEventProducer = approvalEventProducer;
        this.clock = clock;
    }

    @Transactional
    public ApprovalWorkflowView createApprovalWorkflow(CreateApprovalWorkflowCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access =
                approvalPermissionValidationService.requireApprovalCreation(command.workspaceId());
        approvalWorkflowValidationService.requireApprovalWorkflowCreationAllowed(
                command.workspaceId(),
                command.creativeRequestId(),
                command.generatedVersionId());
        if (approvalWorkflowRepository.findByWorkspaceIdAndGeneratedVersionId(
                command.workspaceId(),
                command.generatedVersionId()).isPresent()) {
            throw new BusinessException(ErrorCode.CREATIVE_APPROVAL_DUPLICATE, "Approval workflow already exists for this generated version");
        }

        ApprovalWorkflow workflow = ApprovalWorkflow.create(
                command.workspaceId(),
                command.creativeRequestId(),
                command.generatedVersionId(),
                access.currentUser().userId(),
                command.currentReviewerId());
        ApprovalWorkflow saved = approvalWorkflowRepository.save(workflow);
        synchronizeGeneratedVersion(saved, null);
        cache(saved);
        publish(KafkaTopicConstants.APPROVAL_REQUESTED, saved, access.currentUser().userId(), null);
        return approvalMapper.toView(saved);
    }

    @Transactional(readOnly = true)
    public ApprovalWorkflow requireWorkflow(UUID workspaceId, UUID approvalWorkflowId) {
        return approvalWorkflowValidationService.requireApprovalWorkflowBelongsToWorkspace(workspaceId, approvalWorkflowId);
    }

    @Transactional(readOnly = true)
    public ApprovalWorkflowView getApprovalWorkflow(UUID workspaceId, UUID approvalWorkflowId) {
        approvalPermissionValidationService.requireApprovalVisibility(workspaceId);
        ApprovalWorkflowCacheEntry cached = approvalWorkflowCacheService.getWorkflow(approvalWorkflowId)
                .filter(entry -> workspaceId.equals(entry.workspaceId()))
                .orElse(null);
        if (cached != null) {
            return approvalMapper.toView(cached);
        }
        ApprovalWorkflow workflow = requireWorkflow(workspaceId, approvalWorkflowId);
        approvalWorkflowCacheService.cacheWorkflow(workflow);
        return approvalMapper.toView(workflow);
    }

    void cache(ApprovalWorkflow workflow) {
        approvalWorkflowCacheService.cacheWorkflow(workflow);
        approvalStateCacheService.cacheState(workflow);
        if (workflow.getCurrentReviewerId() != null) {
            reviewerAssignmentCacheService.cacheAssignment(workflow);
        }
    }

    void invalidate(ApprovalWorkflow workflow) {
        approvalWorkflowCacheService.invalidateWorkflow(
                workflow.getWorkspaceId(),
                workflow.getId(),
                workflow.getGeneratedVersionId(),
                workflow.getCurrentReviewerId());
        approvalStateCacheService.invalidateState(
                workflow.getWorkspaceId(),
                workflow.getId(),
                workflow.getGeneratedVersionId(),
                workflow.getCurrentReviewerId());
        reviewerAssignmentCacheService.invalidateAssignment(
                workflow.getWorkspaceId(),
                workflow.getId(),
                workflow.getGeneratedVersionId(),
                workflow.getCurrentReviewerId());
    }

    void synchronizeGeneratedVersion(ApprovalWorkflow workflow, String comments) {
        GeneratedVersionEntity version = generatedVersionRepository
                .findByIdAndWorkspaceIdAndDeletedFalse(workflow.getGeneratedVersionId(), workflow.getWorkspaceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GENERATED_VERSION_NOT_FOUND));
        if (workflow.getCurrentStatus() == ApprovalStatus.PENDING) {
            version.markSubmittedForApproval();
            version.assignReviewer(workflow.getCurrentReviewerId());
        } else if (workflow.getCurrentStatus() == ApprovalStatus.APPROVED) {
            version.markApproved(workflow.getCurrentReviewerId(), comments);
        } else if (workflow.getCurrentStatus() == ApprovalStatus.REJECTED) {
            version.markRejected(workflow.getCurrentReviewerId(), comments, true);
        } else if (workflow.getCurrentStatus() == ApprovalStatus.REVISION_REQUESTED) {
            version.markChangesRequested(workflow.getCurrentReviewerId(), comments);
        } else if (workflow.getCurrentStatus() == ApprovalStatus.IN_REVIEW) {
            version.markInReview(workflow.getCurrentReviewerId(), comments);
        }
        generatedVersionRepository.save(version);
    }

    void publish(String topic, ApprovalWorkflow workflow, UUID actorId, String comments) {
        ApprovalWorkflowEvent event = new ApprovalWorkflowEvent(
                workflow.getId(),
                workflow.getWorkspaceId(),
                workflow.getCreativeRequestId(),
                workflow.getGeneratedVersionId(),
                actorId,
                workflow.getCurrentReviewerId(),
                workflow.getCurrentStatus(),
                comments,
                Instant.now(clock));
        if (KafkaTopicConstants.APPROVAL_REQUESTED.equals(topic)) {
            approvalEventProducer.publishRequested(event);
        } else if (KafkaTopicConstants.APPROVAL_APPROVED.equals(topic)) {
            approvalEventProducer.publishApproved(event);
        } else if (KafkaTopicConstants.APPROVAL_REJECTED.equals(topic)) {
            approvalEventProducer.publishRejected(event);
        } else if (KafkaTopicConstants.APPROVAL_REVISION_REQUESTED.equals(topic)) {
            approvalEventProducer.publishRevisionRequested(event);
        } else {
            throw new BusinessException(ErrorCode.KAFKA_PUBLISH_FAILED, "Unsupported approval event topic");
        }
    }
}
