package com.lebhas.creativesaas.approval.application;

import com.lebhas.approval.event.ApprovalLifecycleEvent;
import com.lebhas.approval.producer.ApprovalEventProducer;
import com.lebhas.creativesaas.approval.application.dto.ApprovalRequestView;
import com.lebhas.creativesaas.approval.application.dto.ApproveApprovalRequestCommand;
import com.lebhas.creativesaas.approval.application.dto.RejectApprovalRequestCommand;
import com.lebhas.creativesaas.approval.application.dto.RequestApprovalChangesCommand;
import com.lebhas.creativesaas.approval.application.dto.StartApprovalReviewCommand;
import com.lebhas.creativesaas.approval.cache.ApprovalCacheService;
import com.lebhas.creativesaas.approval.cache.ApprovalCountCacheService;
import com.lebhas.creativesaas.approval.cache.ApprovalLockService;
import com.lebhas.creativesaas.approval.cache.ApprovalReviewerCacheService;
import com.lebhas.creativesaas.approval.domain.ApprovalAssignment;
import com.lebhas.creativesaas.approval.domain.ApprovalAssignmentStatus;
import com.lebhas.creativesaas.approval.domain.ApprovalAuditAction;
import com.lebhas.creativesaas.approval.domain.ApprovalDecision;
import com.lebhas.creativesaas.approval.domain.ApprovalRequest;
import com.lebhas.creativesaas.approval.domain.ApprovalReview;
import com.lebhas.creativesaas.approval.domain.ApprovalReviewType;
import com.lebhas.creativesaas.approval.domain.ApprovalStatus;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalAssignmentRepository;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalRequestRepository;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalReviewRepository;
import com.lebhas.creativesaas.approval.validation.ApprovalPermissionValidator;
import com.lebhas.creativesaas.approval.validation.ApprovalTransitionValidator;
import com.lebhas.creativesaas.approval.validation.ApprovalWorkspaceValidator;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionService;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.redis.RedisLockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Deprecated(forRemoval = true)
public class ApprovalReviewService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalReviewRepository approvalReviewRepository;
    private final ApprovalAssignmentRepository approvalAssignmentRepository;
    private final GeneratedVersionService generatedVersionService;
    private final ApprovalWorkspaceValidator approvalWorkspaceValidator;
    private final ApprovalPermissionValidator approvalPermissionValidator;
    private final ApprovalTransitionValidator approvalTransitionValidator;
    private final ApprovalAuditService approvalAuditService;
    private final ApprovalCacheService approvalCacheService;
    private final ApprovalCountCacheService approvalCountCacheService;
    private final ApprovalReviewerCacheService approvalReviewerCacheService;
    private final ApprovalLockService approvalLockService;
    private final ApprovalMapper approvalMapper;
    private final ApprovalEventProducer approvalEventProducer;
    private final Clock clock;

    public ApprovalReviewService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            ApprovalRequestRepository approvalRequestRepository,
            ApprovalReviewRepository approvalReviewRepository,
            ApprovalAssignmentRepository approvalAssignmentRepository,
            GeneratedVersionService generatedVersionService,
            ApprovalWorkspaceValidator approvalWorkspaceValidator,
            ApprovalPermissionValidator approvalPermissionValidator,
            ApprovalTransitionValidator approvalTransitionValidator,
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
        this.approvalReviewRepository = approvalReviewRepository;
        this.approvalAssignmentRepository = approvalAssignmentRepository;
        this.generatedVersionService = generatedVersionService;
        this.approvalWorkspaceValidator = approvalWorkspaceValidator;
        this.approvalPermissionValidator = approvalPermissionValidator;
        this.approvalTransitionValidator = approvalTransitionValidator;
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
    public ApprovalRequestView startReview(StartApprovalReviewCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceContext(command.workspaceId());
        ApprovalRequest request = approvalWorkspaceValidator.requireApprovalRequestBelongsToWorkspace(
                command.workspaceId(),
                command.approvalRequestId());
        GeneratedVersionEntity version = approvalWorkspaceValidator.requireGeneratedVersionBelongsToWorkspace(
                command.workspaceId(),
                request.getGeneratedVersionId());
        approvalWorkspaceValidator.requireApprovalRequestMatchesGeneratedVersion(request, version);
        approvalPermissionValidator.requireReviewerActionPermission(
                access.currentUser(),
                access.effectiveRole(),
                access.permissions(),
                command.workspaceId(),
                request);
        validateOptionalFeedback(command.feedback());

        RedisLockService.RedisLockToken reviewToken = acquireReviewToken(
                command.workspaceId(),
                request.getId(),
                version.getId(),
                access.currentUser().userId());
        try {
            ApprovalStatus previousStatus = request.getCurrentStatus();
            approvalTransitionValidator.requireTransition(request, ApprovalStatus.IN_REVIEW);
            request.markInReview(access.currentUser().userId(), clock.instant(), command.feedback());
            request = approvalRequestRepository.save(request);

            version.markInReview(access.currentUser().userId(), command.feedback());
            version = generatedVersionService.save(version);

            ApprovalLifecycleEvent event = ApprovalLifecycleEvent.from(
                    request,
                    access.currentUser().userId(),
                    previousStatus,
                    ApprovalStatus.IN_REVIEW,
                    command.feedback());

            approvalAuditService.record(
                    event.eventId(),
                    command.workspaceId(),
                    request.getId(),
                    version.getId(),
                    access.currentUser().userId(),
                    ApprovalAuditAction.REVIEW_STARTED,
                    previousStatus,
                    ApprovalStatus.IN_REVIEW,
                    command.feedback());

            syncCaches(request, version);
            approvalEventProducer.publishReviewStarted(event);
            return approvalMapper.toView(request);
        } finally {
            releaseReviewToken(reviewToken, command.workspaceId(), request.getId(), version.getId(), access.currentUser().userId());
        }
    }

    @Transactional
    public ApprovalRequestView approve(ApproveApprovalRequestCommand command) {
        return submitDecision(command.workspaceId(), command.approvalRequestId(), command.feedback(), ApprovalDecision.APPROVED);
    }

    @Transactional
    public ApprovalRequestView reject(RejectApprovalRequestCommand command) {
        requireReason(command.feedback(), "rejection reason");
        return submitDecision(command.workspaceId(), command.approvalRequestId(), command.feedback(), ApprovalDecision.REJECTED);
    }

    @Transactional
    public ApprovalRequestView requestChanges(RequestApprovalChangesCommand command) {
        requireReason(command.feedback(), "change request reason");
        return submitDecision(command.workspaceId(), command.approvalRequestId(), command.feedback(), ApprovalDecision.CHANGES_REQUESTED);
    }

    private ApprovalRequestView submitDecision(
            UUID workspaceId,
            UUID approvalRequestId,
            String feedback,
            ApprovalDecision decision
    ) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceContext(workspaceId);
        ApprovalRequest request = approvalWorkspaceValidator.requireApprovalRequestBelongsToWorkspace(workspaceId, approvalRequestId);
        GeneratedVersionEntity version = approvalWorkspaceValidator.requireGeneratedVersionBelongsToWorkspace(
                workspaceId,
                request.getGeneratedVersionId());
        approvalWorkspaceValidator.requireApprovalRequestMatchesGeneratedVersion(request, version);
        approvalPermissionValidator.requireReviewerActionPermission(
                access.currentUser(),
                access.effectiveRole(),
                access.permissions(),
                workspaceId,
                request);
        validateOptionalFeedback(feedback);

        RedisLockService.RedisLockToken reviewToken = acquireReviewToken(
                workspaceId,
                request.getId(),
                version.getId(),
                access.currentUser().userId());
        try {
            Instant now = clock.instant();
            ReviewStartTransition reviewStartTransition = ensureInReview(
                    request,
                    version,
                    access.currentUser().userId(),
                    now,
                    null);
            request = reviewStartTransition.request();
            version = reviewStartTransition.version();
            if (reviewStartTransition.event() != null) {
                approvalEventProducer.publishReviewStarted(reviewStartTransition.event());
            }
            ApprovalStatus previousStatus = request.getCurrentStatus();
            ApprovalStatus targetStatus = toApprovalStatus(decision);
            approvalTransitionValidator.requireTransition(request, targetStatus);

            ApprovalReview review = approvalReviewRepository.save(ApprovalReview.create(
                    workspaceId,
                    request.getId(),
                    access.currentUser().userId(),
                    decision,
                    feedback,
                    toReviewType(request),
                    now));

            request.markReviewed(targetStatus, access.currentUser().userId(), now, feedback);
            request = approvalRequestRepository.save(request);
            version = applyDecision(version, decision, access.currentUser().userId(), feedback);

            closeActiveAssignmentIfPresent(workspaceId, request.getId());

            ApprovalLifecycleEvent event = ApprovalLifecycleEvent.from(
                    request,
                    access.currentUser().userId(),
                    previousStatus,
                    targetStatus,
                    feedback);

            approvalAuditService.record(
                    event.eventId(),
                    workspaceId,
                    request.getId(),
                    version.getId(),
                    access.currentUser().userId(),
                    toAuditAction(decision),
                    previousStatus,
                    targetStatus,
                    feedback);

            syncCaches(request, version);
            publishDecisionEvent(decision, event);
            return approvalMapper.toView(request);
        } finally {
            releaseReviewToken(reviewToken, workspaceId, request.getId(), version.getId(), access.currentUser().userId());
        }
    }

    private ReviewStartTransition ensureInReview(
            ApprovalRequest request,
            GeneratedVersionEntity version,
            UUID actorId,
            Instant now,
            String feedback
    ) {
        if (request.getCurrentStatus() == ApprovalStatus.IN_REVIEW) {
            return new ReviewStartTransition(request, version, null);
        }
        ApprovalStatus previousStatus = request.getCurrentStatus();
        approvalTransitionValidator.requireTransition(request, ApprovalStatus.IN_REVIEW);
        request.markInReview(actorId, now, feedback);
        request = approvalRequestRepository.save(request);
        version.markInReview(actorId, feedback);
        version = generatedVersionService.save(version);
        ApprovalLifecycleEvent event = ApprovalLifecycleEvent.from(
                request,
                actorId,
                previousStatus,
                ApprovalStatus.IN_REVIEW,
                feedback);
        approvalAuditService.record(
                event.eventId(),
                request.getWorkspaceId(),
                request.getId(),
                version.getId(),
                actorId,
                ApprovalAuditAction.REVIEW_STARTED,
                previousStatus,
                ApprovalStatus.IN_REVIEW,
                feedback);
        return new ReviewStartTransition(request, version, event);
    }

    private GeneratedVersionEntity applyDecision(
            GeneratedVersionEntity version,
            ApprovalDecision decision,
            UUID reviewerId,
            String feedback
    ) {
        switch (decision) {
            case APPROVED -> version.markApproved(reviewerId, feedback);
            case REJECTED -> version.markRejected(reviewerId, feedback, false);
            case CHANGES_REQUESTED -> version.markChangesRequested(reviewerId, feedback);
        }
        return generatedVersionService.save(version);
    }

    private void closeActiveAssignmentIfPresent(UUID workspaceId, UUID approvalRequestId) {
        Optional<ApprovalAssignment> activeAssignment = approvalAssignmentRepository
                .findFirstByWorkspaceIdAndApprovalRequestIdAndAssignmentStatusAndDeletedFalseOrderByAssignedAtDesc(
                        workspaceId,
                        approvalRequestId,
                        ApprovalAssignmentStatus.ACTIVE);
        activeAssignment.ifPresent(assignment -> {
            assignment.updateAssignmentStatus(ApprovalAssignmentStatus.COMPLETED);
            approvalAssignmentRepository.save(assignment);
        });
    }

    private ApprovalReviewType toReviewType(ApprovalRequest request) {
        return request.getRevisionCount() > 0 || request.getCurrentStatus() == ApprovalStatus.RESUBMITTED
                ? ApprovalReviewType.RESUBMISSION
                : ApprovalReviewType.INITIAL;
    }

    private ApprovalAuditAction toAuditAction(ApprovalDecision decision) {
        return switch (decision) {
            case APPROVED -> ApprovalAuditAction.APPROVED;
            case REJECTED -> ApprovalAuditAction.REJECTED;
            case CHANGES_REQUESTED -> ApprovalAuditAction.CHANGES_REQUESTED;
        };
    }

    private ApprovalStatus toApprovalStatus(ApprovalDecision decision) {
        return switch (decision) {
            case APPROVED -> ApprovalStatus.APPROVED;
            case REJECTED -> ApprovalStatus.REJECTED;
            case CHANGES_REQUESTED -> ApprovalStatus.CHANGES_REQUESTED;
        };
    }

    private RedisLockService.RedisLockToken acquireReviewToken(
            UUID workspaceId,
            UUID approvalRequestId,
            UUID generatedVersionId,
            UUID reviewerId
    ) {
        return approvalLockService.acquireReviewSubmissionLock(workspaceId, approvalRequestId, generatedVersionId, reviewerId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.CREATIVE_APPROVAL_REVIEW_IN_PROGRESS,
                        "Another approval review submission is already in progress"));
    }

    private void releaseReviewToken(
            RedisLockService.RedisLockToken token,
            UUID workspaceId,
            UUID approvalRequestId,
            UUID generatedVersionId,
            UUID reviewerId
    ) {
        approvalLockService.releaseLock(token, workspaceId, approvalRequestId, generatedVersionId, reviewerId);
    }

    private void requireReason(String feedback, String label) {
        if (!StringUtils.hasText(feedback)) {
            throw new BusinessException(ErrorCode.CREATIVE_APPROVAL_REASON_REQUIRED, label + " is required");
        }
        validateOptionalFeedback(feedback);
    }

    private void validateOptionalFeedback(String feedback) {
        if (feedback == null) {
            return;
        }
        if (feedback.trim().length() > 2000) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Approval feedback must be 2000 characters or fewer");
        }
    }

    private void publishDecisionEvent(ApprovalDecision decision, ApprovalLifecycleEvent event) {
        switch (decision) {
            case APPROVED -> approvalEventProducer.publishApproved(event);
            case REJECTED -> approvalEventProducer.publishRejected(event);
            case CHANGES_REQUESTED -> approvalEventProducer.publishChangesRequested(event);
        }
    }

    private void syncCaches(ApprovalRequest request, GeneratedVersionEntity version) {
        approvalCacheService.cacheApprovalRequest(approvalMapper.toRequestCacheEntry(request));
        approvalCacheService.cacheApprovalStatus(approvalMapper.toStatusCacheEntry(request, version));
        approvalCountCacheService.invalidatePendingApprovals(request.getWorkspaceId());
        if (request.getAssignedReviewerId() != null) {
            approvalReviewerCacheService.invalidateReviewerQueue(request.getAssignedReviewerId());
        }
    }

    private record ReviewStartTransition(
            ApprovalRequest request,
            GeneratedVersionEntity version,
            ApprovalLifecycleEvent event
    ) {
    }
}
