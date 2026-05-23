package com.lebhas.creativesaas.approval.application;

import com.lebhas.approval.event.ApprovalLifecycleEvent;
import com.lebhas.approval.producer.ApprovalEventProducer;
import com.lebhas.creativesaas.approval.application.dto.ApprovalRequestView;
import com.lebhas.creativesaas.approval.application.dto.ResubmitApprovalRequestCommand;
import com.lebhas.creativesaas.approval.application.dto.SubmitApprovalRequestCommand;
import com.lebhas.creativesaas.approval.cache.ApprovalCacheService;
import com.lebhas.creativesaas.approval.cache.ApprovalCountCacheService;
import com.lebhas.creativesaas.approval.cache.ApprovalLockService;
import com.lebhas.creativesaas.approval.cache.ApprovalReviewerCacheService;
import com.lebhas.creativesaas.approval.domain.ApprovalAssignment;
import com.lebhas.creativesaas.approval.domain.ApprovalAssignmentStatus;
import com.lebhas.creativesaas.approval.domain.ApprovalAuditAction;
import com.lebhas.creativesaas.approval.domain.ApprovalRequest;
import com.lebhas.creativesaas.approval.domain.ApprovalStatus;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalAssignmentRepository;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalRequestRepository;
import com.lebhas.creativesaas.approval.validation.ApprovalPermissionValidator;
import com.lebhas.creativesaas.approval.validation.ApprovalTransitionValidator;
import com.lebhas.creativesaas.approval.validation.ApprovalWorkspaceValidator;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionService;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.domain.GenerationStatus;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.redis.RedisLockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Deprecated(forRemoval = true)
public class ApprovalRequestService {

    private static final Set<ApprovalStatus> ACTIVE_APPROVAL_STATUSES = Set.of(
            ApprovalStatus.SUBMITTED,
            ApprovalStatus.IN_REVIEW,
            ApprovalStatus.RESUBMITTED,
            ApprovalStatus.CHANGES_REQUESTED);

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalAssignmentRepository approvalAssignmentRepository;
    private final GeneratedVersionService generatedVersionService;
    private final ApprovalWorkspaceValidator approvalWorkspaceValidator;
    private final ApprovalTransitionValidator approvalTransitionValidator;
    private final ApprovalPermissionValidator approvalPermissionValidator;
    private final ApprovalAuditService approvalAuditService;
    private final ApprovalCacheService approvalCacheService;
    private final ApprovalCountCacheService approvalCountCacheService;
    private final ApprovalReviewerCacheService approvalReviewerCacheService;
    private final ApprovalLockService approvalLockService;
    private final ApprovalMapper approvalMapper;
    private final ApprovalEventProducer approvalEventProducer;
    private final Clock clock;

    public ApprovalRequestService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            ApprovalRequestRepository approvalRequestRepository,
            ApprovalAssignmentRepository approvalAssignmentRepository,
            GeneratedVersionService generatedVersionService,
            ApprovalWorkspaceValidator approvalWorkspaceValidator,
            ApprovalTransitionValidator approvalTransitionValidator,
            ApprovalPermissionValidator approvalPermissionValidator,
            ApprovalAuditService approvalAuditService,
            ApprovalCacheService approvalCacheService,
            ApprovalCountCacheService approvalCountCacheService,
            ApprovalReviewerCacheService approvalReviewerCacheService,
            ApprovalLockService approvalLockService,
            ApprovalMapper approvalMapper,
            ApprovalEventProducer approvalEventProducer,
            Clock clock
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.approvalRequestRepository = approvalRequestRepository;
        this.approvalAssignmentRepository = approvalAssignmentRepository;
        this.generatedVersionService = generatedVersionService;
        this.approvalWorkspaceValidator = approvalWorkspaceValidator;
        this.approvalTransitionValidator = approvalTransitionValidator;
        this.approvalPermissionValidator = approvalPermissionValidator;
        this.approvalAuditService = approvalAuditService;
        this.approvalCacheService = approvalCacheService;
        this.approvalCountCacheService = approvalCountCacheService;
        this.approvalReviewerCacheService = approvalReviewerCacheService;
        this.approvalLockService = approvalLockService;
        this.approvalMapper = approvalMapper;
        this.approvalEventProducer = approvalEventProducer;
        this.clock = clock;
    }

    @Transactional
    public ApprovalRequestView submit(SubmitApprovalRequestCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access =
                workspaceAuthorizationService.requirePermission(command.workspaceId(), Permission.CREATIVE_SUBMIT);
        validateDueAt(command.dueAt());

        GeneratedVersionEntity version = approvalWorkspaceValidator.requireGeneratedVersionBelongsToWorkspace(
                command.workspaceId(),
                command.generatedVersionId());
        if (version.getGenerationStatus() != GenerationStatus.READY) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Generated version is not ready for approval");
        }
        approvalTransitionValidator.requireTransition(version, ApprovalStatus.SUBMITTED);
        if (approvalRequestRepository.existsByWorkspaceIdAndGeneratedVersionIdAndCurrentStatusInAndDeletedFalse(
                command.workspaceId(),
                command.generatedVersionId(),
                ACTIVE_APPROVAL_STATUSES)) {
            throw new BusinessException(
                    ErrorCode.CREATIVE_APPROVAL_DUPLICATE,
                    "An active approval request already exists for this generated version");
        }

        Instant now = clock.instant();
        ApprovalRequest request = ApprovalRequest.create(
                command.workspaceId(),
                version.getId(),
                version.getProjectCampaignId(),
                access.currentUser().userId(),
                null,
                ApprovalStatus.SUBMITTED,
                now,
                null,
                command.dueAt(),
                command.submissionComment(),
                version.getRevisionNumber());
        request = approvalRequestRepository.save(request);

        version.markSubmittedForApproval();
        if (StringUtils.hasText(command.submissionComment())) {
            version.recordApprovalComment(null, command.submissionComment());
        }
        version = generatedVersionService.save(version);

        ApprovalLifecycleEvent event = ApprovalLifecycleEvent.from(
                request,
                access.currentUser().userId(),
                ApprovalStatus.NOT_SUBMITTED,
                ApprovalStatus.SUBMITTED,
                command.submissionComment());

        approvalAuditService.record(
                event.eventId(),
                command.workspaceId(),
                request.getId(),
                version.getId(),
                access.currentUser().userId(),
                ApprovalAuditAction.SUBMITTED,
                ApprovalStatus.NOT_SUBMITTED,
                ApprovalStatus.SUBMITTED,
                command.submissionComment());

        syncCaches(request, version, null);
        approvalEventProducer.publishRequestSubmitted(event);
        return approvalMapper.toView(request);
    }

    @Transactional
    public ApprovalRequestView resubmit(ResubmitApprovalRequestCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceContext(command.workspaceId());
        approvalPermissionValidator.requireApprovalVisibility(
                access.currentUser(),
                access.effectiveRole(),
                access.permissions(),
                command.workspaceId());

        ApprovalRequest request = approvalWorkspaceValidator.requireApprovalRequestBelongsToWorkspace(
                command.workspaceId(),
                command.approvalRequestId());
        GeneratedVersionEntity version = approvalWorkspaceValidator.requireGeneratedVersionBelongsToWorkspace(
                command.workspaceId(),
                request.getGeneratedVersionId());
        approvalWorkspaceValidator.requireApprovalRequestMatchesGeneratedVersion(request, version);
        requireRequestMutator(access, request);
        approvalTransitionValidator.requireTransition(request, ApprovalStatus.RESUBMITTED);
        approvalTransitionValidator.requireResubmissionAllowed(version);

        RedisLockService.RedisLockToken revisionToken = approvalLockService.acquireRevisionLock(
                        command.workspaceId(),
                        request.getId(),
                        version.getId(),
                        access.currentUser().userId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.CREATIVE_APPROVAL_REVIEW_IN_PROGRESS,
                        "Another approval resubmission is already in progress"));
        try {
            ApprovalStatus previousStatus = request.getCurrentStatus();
            Instant now = clock.instant();
            request.markResubmitted(now, command.resubmissionComment());
            request = approvalRequestRepository.save(request);

            version.markResubmitted(command.resubmissionComment());
            version = generatedVersionService.save(version);

            reopenAssignmentIfNeeded(request, access.currentUser().userId(), now);

            ApprovalLifecycleEvent event = ApprovalLifecycleEvent.from(
                    request,
                    access.currentUser().userId(),
                    previousStatus,
                    ApprovalStatus.RESUBMITTED,
                    command.resubmissionComment());

            approvalAuditService.record(
                    event.eventId(),
                    command.workspaceId(),
                    request.getId(),
                    version.getId(),
                    access.currentUser().userId(),
                    ApprovalAuditAction.RESUBMITTED,
                    previousStatus,
                    ApprovalStatus.RESUBMITTED,
                    command.resubmissionComment());

            syncCaches(request, version, request.getAssignedReviewerId());
            approvalEventProducer.publishResubmitted(event);
            return approvalMapper.toView(request);
        } finally {
            approvalLockService.releaseLock(
                    revisionToken,
                    command.workspaceId(),
                    request.getId(),
                    version.getId(),
                    access.currentUser().userId());
        }
    }

    private void reopenAssignmentIfNeeded(ApprovalRequest request, UUID actorId, Instant assignedAt) {
        if (request.getAssignedReviewerId() == null) {
            return;
        }
        approvalAssignmentRepository.save(ApprovalAssignment.create(
                request.getWorkspaceId(),
                request.getId(),
                request.getAssignedReviewerId(),
                actorId,
                assignedAt,
                ApprovalAssignmentStatus.ACTIVE));
        approvalReviewerCacheService.invalidateReviewerQueue(request.getAssignedReviewerId());
    }

    private void validateDueAt(Instant dueAt) {
        if (dueAt != null && dueAt.isBefore(clock.instant())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "dueAt must be in the future");
        }
    }

    private void requireRequestMutator(
            WorkspaceAuthorizationService.WorkspaceAccess access,
            ApprovalRequest request
    ) {
        if (access.currentUser().isMaster()) {
            approvalPermissionValidator.requireMasterSupportVisibility(
                    access.currentUser(),
                    access.permissions(),
                    request.getWorkspaceId());
            return;
        }
        if (access.effectiveRole() == Role.ADMIN && access.permissions().contains(Permission.CREATIVE_SUBMIT)) {
            return;
        }
        if (Objects.equals(request.getSubmittedBy(), access.currentUser().userId())
                && access.permissions().contains(Permission.CREATIVE_SUBMIT)) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "Approval request can only be resubmitted by the submitter or an administrator");
    }

    private void syncCaches(ApprovalRequest request, GeneratedVersionEntity version, UUID reviewerToInvalidate) {
        approvalCacheService.cacheApprovalRequest(approvalMapper.toRequestCacheEntry(request));
        approvalCacheService.cacheApprovalStatus(approvalMapper.toStatusCacheEntry(request, version));
        approvalCountCacheService.invalidatePendingApprovals(request.getWorkspaceId());
        if (reviewerToInvalidate != null) {
            approvalReviewerCacheService.invalidateReviewerQueue(reviewerToInvalidate);
        }
    }
}
