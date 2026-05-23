package com.lebhas.creativesaas.approval.application;

import com.lebhas.approval.event.ApprovalLifecycleEvent;
import com.lebhas.approval.producer.ApprovalEventProducer;
import com.lebhas.creativesaas.approval.application.dto.ApprovalAssignmentView;
import com.lebhas.creativesaas.approval.application.dto.AssignApprovalRequestCommand;
import com.lebhas.creativesaas.approval.cache.ApprovalCacheService;
import com.lebhas.creativesaas.approval.cache.ApprovalReviewerCacheService;
import com.lebhas.creativesaas.approval.domain.ApprovalAssignment;
import com.lebhas.creativesaas.approval.domain.ApprovalAssignmentStatus;
import com.lebhas.creativesaas.approval.domain.ApprovalAuditAction;
import com.lebhas.creativesaas.approval.domain.ApprovalRequest;
import com.lebhas.creativesaas.approval.domain.ApprovalStatus;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalAssignmentRepository;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalRequestRepository;
import com.lebhas.creativesaas.approval.validation.ApprovalPermissionValidator;
import com.lebhas.creativesaas.approval.validation.ApprovalReviewerValidator;
import com.lebhas.creativesaas.approval.validation.ApprovalStateMachine;
import com.lebhas.creativesaas.approval.validation.ApprovalWorkspaceValidator;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionService;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Service
@Deprecated(forRemoval = true)
public class ApprovalAssignmentService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalAssignmentRepository approvalAssignmentRepository;
    private final GeneratedVersionService generatedVersionService;
    private final ApprovalWorkspaceValidator approvalWorkspaceValidator;
    private final ApprovalReviewerValidator approvalReviewerValidator;
    private final ApprovalPermissionValidator approvalPermissionValidator;
    private final ApprovalStateMachine approvalStateMachine;
    private final ApprovalAuditService approvalAuditService;
    private final ApprovalCacheService approvalCacheService;
    private final ApprovalReviewerCacheService approvalReviewerCacheService;
    private final ApprovalMapper approvalMapper;
    private final ApprovalEventProducer approvalEventProducer;
    private final Clock clock;

    public ApprovalAssignmentService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            ApprovalRequestRepository approvalRequestRepository,
            ApprovalAssignmentRepository approvalAssignmentRepository,
            GeneratedVersionService generatedVersionService,
            ApprovalWorkspaceValidator approvalWorkspaceValidator,
            ApprovalReviewerValidator approvalReviewerValidator,
            ApprovalPermissionValidator approvalPermissionValidator,
            ApprovalStateMachine approvalStateMachine,
            ApprovalAuditService approvalAuditService,
            ApprovalCacheService approvalCacheService,
            ApprovalReviewerCacheService approvalReviewerCacheService,
            ApprovalMapper approvalMapper,
            ApprovalEventProducer approvalEventProducer,
            Clock clock
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.approvalRequestRepository = approvalRequestRepository;
        this.approvalAssignmentRepository = approvalAssignmentRepository;
        this.generatedVersionService = generatedVersionService;
        this.approvalWorkspaceValidator = approvalWorkspaceValidator;
        this.approvalReviewerValidator = approvalReviewerValidator;
        this.approvalPermissionValidator = approvalPermissionValidator;
        this.approvalStateMachine = approvalStateMachine;
        this.approvalAuditService = approvalAuditService;
        this.approvalCacheService = approvalCacheService;
        this.approvalReviewerCacheService = approvalReviewerCacheService;
        this.approvalMapper = approvalMapper;
        this.approvalEventProducer = approvalEventProducer;
        this.clock = clock;
    }

    @Transactional
    public ApprovalAssignmentView assignReviewer(AssignApprovalRequestCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceContext(command.workspaceId());
        approvalPermissionValidator.requireAssignmentPermission(
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

        if (approvalStateMachine.isTerminal(request.getCurrentStatus())) {
            throw new BusinessException(ErrorCode.CREATIVE_APPROVAL_INVALID_TRANSITION, "Reviewer assignment is not allowed for a completed approval request");
        }

        approvalReviewerValidator.requireReviewerPermission(
                approvalReviewerValidator.requireReviewerBelongsToWorkspace(command.workspaceId(), command.reviewerId()));

        Optional<ApprovalAssignment> currentAssignment = approvalAssignmentRepository
                .findFirstByWorkspaceIdAndApprovalRequestIdAndAssignmentStatusAndDeletedFalseOrderByAssignedAtDesc(
                        command.workspaceId(),
                        command.approvalRequestId(),
                        ApprovalAssignmentStatus.ACTIVE);
        if (currentAssignment.isPresent() && Objects.equals(currentAssignment.get().getAssignedTo(), command.reviewerId())) {
            return approvalMapper.toView(currentAssignment.get());
        }

        Instant now = clock.instant();
        ApprovalStatus currentStatus = request.getCurrentStatus();
        var previousReviewerId = request.getAssignedReviewerId();

        currentAssignment.ifPresent(existing -> {
            existing.updateAssignmentStatus(ApprovalAssignmentStatus.REASSIGNED);
            approvalAssignmentRepository.save(existing);
        });

        request.assignReviewer(command.reviewerId());
        approvalRequestRepository.save(request);

        version.assignReviewer(command.reviewerId());
        generatedVersionService.save(version);

        ApprovalAssignment assignment = approvalAssignmentRepository.save(ApprovalAssignment.create(
                command.workspaceId(),
                request.getId(),
                command.reviewerId(),
                access.currentUser().userId(),
                now,
                ApprovalAssignmentStatus.ACTIVE));

        String details = previousReviewerId == null
                ? "Reviewer assigned to " + command.reviewerId()
                : "Reviewer reassigned from " + previousReviewerId + " to " + command.reviewerId();
        ApprovalLifecycleEvent event = ApprovalLifecycleEvent.from(
                request,
                access.currentUser().userId(),
                currentStatus,
                currentStatus,
                details,
                false,
                previousReviewerId);

        approvalAuditService.record(
                event.eventId(),
                command.workspaceId(),
                request.getId(),
                version.getId(),
                access.currentUser().userId(),
                event.isReassignment() ? ApprovalAuditAction.REASSIGNED : ApprovalAuditAction.ASSIGNED,
                currentStatus,
                currentStatus,
                details);

        approvalCacheService.cacheApprovalRequest(approvalMapper.toRequestCacheEntry(request));
        approvalCacheService.cacheApprovalStatus(approvalMapper.toStatusCacheEntry(request, version));
        if (previousReviewerId != null) {
            approvalReviewerCacheService.invalidateReviewerQueue(previousReviewerId);
        }
        approvalReviewerCacheService.invalidateReviewerQueue(command.reviewerId());
        approvalEventProducer.publishAssigned(event);
        return approvalMapper.toView(assignment);
    }
}
