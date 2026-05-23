package com.lebhas.creativesaas.approval.application;

import com.lebhas.creativesaas.approval.application.dto.AddCreativeReviewCommentCommand;
import com.lebhas.creativesaas.approval.application.dto.CreateCreativeApprovalCommand;
import com.lebhas.creativesaas.approval.application.dto.CreativeApprovalHistoryView;
import com.lebhas.creativesaas.approval.application.dto.CreativeApprovalListCriteria;
import com.lebhas.creativesaas.approval.application.dto.CreativeApprovalView;
import com.lebhas.creativesaas.approval.application.dto.CreativeReviewCommentView;
import com.lebhas.creativesaas.approval.domain.CreativeApprovalAction;
import com.lebhas.creativesaas.approval.domain.CreativeApprovalEntity;
import com.lebhas.creativesaas.approval.domain.CreativeApprovalHistoryEntity;
import com.lebhas.creativesaas.approval.domain.CreativeApprovalStatus;
import com.lebhas.creativesaas.approval.domain.CreativeReviewCommentEntity;
import com.lebhas.creativesaas.approval.infrastructure.persistence.CreativeApprovalHistoryRepository;
import com.lebhas.creativesaas.approval.infrastructure.persistence.CreativeApprovalRepository;
import com.lebhas.creativesaas.approval.infrastructure.persistence.CreativeApprovalSpecifications;
import com.lebhas.creativesaas.approval.infrastructure.persistence.CreativeReviewCommentRepository;
import com.lebhas.creativesaas.common.api.PagedResult;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.generation.domain.CreativeGenerationRequestEntity;
import com.lebhas.creativesaas.generation.domain.CreativeGenerationStatus;
import com.lebhas.creativesaas.generation.domain.CreativeOutputEntity;
import com.lebhas.creativesaas.generation.infrastructure.persistence.CreativeGenerationRequestRepository;
import com.lebhas.creativesaas.generation.infrastructure.persistence.CreativeOutputRepository;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Deprecated(forRemoval = true)
public class CreativeApprovalService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MIN_COMMENT_LENGTH = 2;
    private static final int MAX_COMMENT_LENGTH = 2000;
    private static final Set<CreativeApprovalStatus> ACTIVE_STATUSES = Set.of(
            CreativeApprovalStatus.DRAFT,
            CreativeApprovalStatus.SUBMITTED,
            CreativeApprovalStatus.IN_REVIEW,
            CreativeApprovalStatus.REGENERATE_REQUESTED);

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final CreativeApprovalRepository approvalRepository;
    private final CreativeReviewCommentRepository commentRepository;
    private final CreativeApprovalHistoryRepository historyRepository;
    private final CreativeOutputRepository outputRepository;
    private final CreativeGenerationRequestRepository generationRequestRepository;
    private final CreativeApprovalTransitionValidator transitionValidator;
    private final CreativeApprovalViewMapper viewMapper;
    private final CreativeApprovalActivityLogger activityLogger;
    private final CreativeApprovalNotificationPublisher notificationPublisher;

    public CreativeApprovalService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            CreativeApprovalRepository approvalRepository,
            CreativeReviewCommentRepository commentRepository,
            CreativeApprovalHistoryRepository historyRepository,
            CreativeOutputRepository outputRepository,
            CreativeGenerationRequestRepository generationRequestRepository,
            CreativeApprovalTransitionValidator transitionValidator,
            CreativeApprovalViewMapper viewMapper,
            CreativeApprovalActivityLogger activityLogger,
            CreativeApprovalNotificationPublisher notificationPublisher
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.approvalRepository = approvalRepository;
        this.commentRepository = commentRepository;
        this.historyRepository = historyRepository;
        this.outputRepository = outputRepository;
        this.generationRequestRepository = generationRequestRepository;
        this.transitionValidator = transitionValidator;
        this.viewMapper = viewMapper;
        this.activityLogger = activityLogger;
        this.notificationPublisher = notificationPublisher;
    }

    @Transactional
    public CreativeApprovalView create(CreateCreativeApprovalCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requireSubmitAccess(command.workspaceId());
        CreativeOutputEntity output = requireOutput(command.workspaceId(), command.creativeOutputId());
        CreativeGenerationRequestEntity generationRequest = requireGenerationRequest(command.workspaceId(), output.getRequestId());
        requireOutputVisibleToActor(output, generationRequest, access);
        validateApprovalNote(command.workspaceId(), access.currentUser().userId(), command.approvalNote());
        validateDueAt(command.workspaceId(), access.currentUser().userId(), command.dueAt());
        if (approvalRepository.existsByWorkspaceIdAndCreativeOutputIdAndStatusInAndDeletedFalse(
                command.workspaceId(), command.creativeOutputId(), ACTIVE_STATUSES)) {
            activityLogger.logValidationFailure(command.workspaceId(), access.currentUser().userId(), "duplicate_active_approval");
            throw new BusinessException(ErrorCode.CREATIVE_APPROVAL_DUPLICATE);
        }

        CreativeApprovalEntity approval = CreativeApprovalEntity.draft(
                command.workspaceId(),
                output.getProjectId() == null ? generationRequest.getProjectId() : output.getProjectId(),
                output.getId(),
                generationRequest.getId(),
                access.currentUser().userId(),
                command.priority(),
                command.dueAt(),
                command.approvalNote());
        approvalRepository.save(approval);
        activityLogger.logApprovalCreated(command.workspaceId(), approval.getId(), output.getId(), access.currentUser().userId());
        return viewMapper.toApprovalView(approval);
    }

    @Transactional(readOnly = true)
    public PagedResult<CreativeApprovalView> list(CreativeApprovalListCriteria criteria) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requireSubmitAccess(criteria.workspaceId());
        UUID submittedBy = visibleSubmittedByFilter(access, criteria.submittedBy());
        CreativeApprovalListCriteria scopedCriteria = new CreativeApprovalListCriteria(
                criteria.workspaceId(),
                criteria.creativeOutputId(),
                criteria.generationRequestId(),
                submittedBy,
                criteria.reviewedBy(),
                criteria.status(),
                criteria.priority(),
                criteria.page(),
                criteria.size());
        return PagedResult.from(approvalRepository.findAll(
                        CreativeApprovalSpecifications.forList(scopedCriteria),
                        PageRequest.of(
                                Math.max(scopedCriteria.page(), 0),
                                Math.min(scopedCriteria.size() <= 0 ? DEFAULT_PAGE_SIZE : scopedCriteria.size(), MAX_PAGE_SIZE),
                                Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(viewMapper::toApprovalView));
    }

    @Transactional(readOnly = true)
    public CreativeApprovalView get(UUID workspaceId, UUID approvalId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requireSubmitAccess(workspaceId);
        return viewMapper.toApprovalView(requireVisibleApproval(workspaceId, approvalId, access));
    }

    @Transactional
    public CreativeApprovalView submit(UUID workspaceId, UUID approvalId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requireSubmitAccess(workspaceId);
        CreativeApprovalEntity approval = requireVisibleApproval(workspaceId, approvalId, access);
        CreativeApprovalStatus previous = approval.getStatus();
        applyTransition(approval, access.currentUser().userId(), CreativeApprovalStatus.SUBMITTED);
        approval.submit();
        approvalRepository.save(approval);
        recordHistory(approval, CreativeApprovalAction.SUBMITTED, previous, approval.getStatus(), access.currentUser().userId(), null);
        emitNotification(approval, CreativeApprovalAction.SUBMITTED, access.currentUser().userId());
        activityLogger.logAction(workspaceId, approvalId, approval.getCreativeOutputId(), access.currentUser().userId(), CreativeApprovalAction.SUBMITTED);
        return viewMapper.toApprovalView(approval);
    }

    @Transactional
    public CreativeApprovalView startReview(UUID workspaceId, UUID approvalId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requireReviewerAccess(workspaceId);
        CreativeApprovalEntity approval = requireVisibleApproval(workspaceId, approvalId, access);
        CreativeApprovalStatus previous = approval.getStatus();
        applyTransition(approval, access.currentUser().userId(), CreativeApprovalStatus.IN_REVIEW);
        approval.startReview(access.currentUser().userId());
        approvalRepository.save(approval);
        recordHistory(approval, CreativeApprovalAction.REVIEW_STARTED, previous, approval.getStatus(), access.currentUser().userId(), null);
        emitNotification(approval, CreativeApprovalAction.REVIEW_STARTED, access.currentUser().userId());
        activityLogger.logAction(workspaceId, approvalId, approval.getCreativeOutputId(), access.currentUser().userId(), CreativeApprovalAction.REVIEW_STARTED);
        return viewMapper.toApprovalView(approval);
    }

    @Transactional
    public CreativeApprovalView approve(UUID workspaceId, UUID approvalId, String approvalNote) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requireReviewerAccess(workspaceId);
        CreativeApprovalEntity approval = requireVisibleApproval(workspaceId, approvalId, access);
        validateOptionalReviewText(workspaceId, access.currentUser().userId(), approvalNote, "approval note");
        CreativeApprovalStatus previous = approval.getStatus();
        applyTransition(approval, access.currentUser().userId(), CreativeApprovalStatus.APPROVED);
        approval.approve(access.currentUser().userId(), approvalNote);
        approvalRepository.save(approval);
        recordHistory(approval, CreativeApprovalAction.APPROVED, previous, approval.getStatus(), access.currentUser().userId(), approvalNote);
        emitNotification(approval, CreativeApprovalAction.APPROVED, access.currentUser().userId());
        activityLogger.logAction(workspaceId, approvalId, approval.getCreativeOutputId(), access.currentUser().userId(), CreativeApprovalAction.APPROVED);
        return viewMapper.toApprovalView(approval);
    }

    @Transactional
    public CreativeApprovalView reject(UUID workspaceId, UUID approvalId, String rejectionReason) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requireReviewerAccess(workspaceId);
        CreativeApprovalEntity approval = requireVisibleApproval(workspaceId, approvalId, access);
        requireReviewText(workspaceId, access.currentUser().userId(), rejectionReason, "rejection reason");
        CreativeApprovalStatus previous = approval.getStatus();
        applyTransition(approval, access.currentUser().userId(), CreativeApprovalStatus.REJECTED);
        approval.reject(access.currentUser().userId(), rejectionReason);
        approvalRepository.save(approval);
        recordHistory(approval, CreativeApprovalAction.REJECTED, previous, approval.getStatus(), access.currentUser().userId(), rejectionReason);
        emitNotification(approval, CreativeApprovalAction.REJECTED, access.currentUser().userId());
        activityLogger.logAction(workspaceId, approvalId, approval.getCreativeOutputId(), access.currentUser().userId(), CreativeApprovalAction.REJECTED);
        return viewMapper.toApprovalView(approval);
    }

    @Transactional
    public CreativeApprovalView requestRegenerate(UUID workspaceId, UUID approvalId, String regenerateInstruction) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requireReviewerAccess(workspaceId);
        CreativeApprovalEntity approval = requireVisibleApproval(workspaceId, approvalId, access);
        requireReviewText(workspaceId, access.currentUser().userId(), regenerateInstruction, "regenerate instruction");
        CreativeApprovalStatus previous = approval.getStatus();
        applyTransition(approval, access.currentUser().userId(), CreativeApprovalStatus.REGENERATE_REQUESTED);
        approval.requestRegenerate(access.currentUser().userId(), regenerateInstruction);
        approvalRepository.save(approval);
        recordHistory(approval, CreativeApprovalAction.REGENERATE_REQUESTED, previous, approval.getStatus(), access.currentUser().userId(), regenerateInstruction);
        emitNotification(approval, CreativeApprovalAction.REGENERATE_REQUESTED, access.currentUser().userId());
        activityLogger.logAction(workspaceId, approvalId, approval.getCreativeOutputId(), access.currentUser().userId(), CreativeApprovalAction.REGENERATE_REQUESTED);
        return viewMapper.toApprovalView(approval);
    }

    @Transactional
    public CreativeApprovalView cancel(UUID workspaceId, UUID approvalId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requireSubmitAccess(workspaceId);
        CreativeApprovalEntity approval = requireVisibleApproval(workspaceId, approvalId, access);
        if (!isReviewer(access) && !approval.getSubmittedBy().equals(access.currentUser().userId())) {
            activityLogger.logAuthorizationFailure(workspaceId, access.currentUser().userId(), "approval_cancel_owner_or_reviewer_required");
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED);
        }
        CreativeApprovalStatus previous = approval.getStatus();
        applyTransition(approval, access.currentUser().userId(), CreativeApprovalStatus.CANCELLED);
        approval.cancel();
        approvalRepository.save(approval);
        recordHistory(approval, CreativeApprovalAction.CANCELLED, previous, approval.getStatus(), access.currentUser().userId(), null);
        activityLogger.logAction(workspaceId, approvalId, approval.getCreativeOutputId(), access.currentUser().userId(), CreativeApprovalAction.CANCELLED);
        return viewMapper.toApprovalView(approval);
    }

    @Transactional
    public CreativeReviewCommentView addComment(AddCreativeReviewCommentCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requireSubmitAccess(command.workspaceId());
        CreativeApprovalEntity approval = requireVisibleApproval(command.workspaceId(), command.approvalId(), access);
        validateComment(command.workspaceId(), access.currentUser().userId(), command.comment());
        CreativeReviewCommentEntity comment = CreativeReviewCommentEntity.create(
                command.workspaceId(),
                approval.getId(),
                approval.getCreativeOutputId(),
                access.currentUser().userId(),
                command.comment(),
                command.commentType());
        commentRepository.save(comment);
        recordHistory(approval, CreativeApprovalAction.COMMENT_ADDED, approval.getStatus(), approval.getStatus(), access.currentUser().userId(), command.comment());
        emitNotification(approval, CreativeApprovalAction.COMMENT_ADDED, access.currentUser().userId());
        activityLogger.logCommentAdded(command.workspaceId(), approval.getId(), approval.getCreativeOutputId(), access.currentUser().userId(), command.comment().trim().length());
        return viewMapper.toCommentView(comment);
    }

    @Transactional(readOnly = true)
    public List<CreativeReviewCommentView> comments(UUID workspaceId, UUID approvalId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requireSubmitAccess(workspaceId);
        CreativeApprovalEntity approval = requireVisibleApproval(workspaceId, approvalId, access);
        return commentRepository.findByWorkspaceIdAndApprovalIdAndDeletedFalseOrderByCreatedAtAsc(workspaceId, approval.getId())
                .stream()
                .map(viewMapper::toCommentView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CreativeApprovalHistoryView> history(UUID workspaceId, UUID approvalId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requireSubmitAccess(workspaceId);
        CreativeApprovalEntity approval = requireVisibleApproval(workspaceId, approvalId, access);
        return historyRepository.findByWorkspaceIdAndApprovalIdOrderByCreatedAtAsc(workspaceId, approval.getId())
                .stream()
                .map(viewMapper::toHistoryView)
                .toList();
    }

    private WorkspaceAuthorizationService.WorkspaceAccess requireSubmitAccess(UUID workspaceId) {
        return workspaceAuthorizationService.requirePermission(workspaceId, Permission.CREATIVE_SUBMIT);
    }

    private WorkspaceAuthorizationService.WorkspaceAccess requireReviewerAccess(UUID workspaceId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requireSubmitAccess(workspaceId);
        if (isReviewer(access)) {
            return access;
        }
        activityLogger.logAuthorizationFailure(workspaceId, access.currentUser().userId(), "reviewer_role_required");
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    private boolean isReviewer(WorkspaceAuthorizationService.WorkspaceAccess access) {
        return access.currentUser().isMaster() || access.effectiveRole() == Role.ADMIN;
    }

    private UUID visibleSubmittedByFilter(WorkspaceAuthorizationService.WorkspaceAccess access, UUID requestedSubmittedBy) {
        if (isReviewer(access)) {
            return requestedSubmittedBy;
        }
        return access.currentUser().userId();
    }

    private CreativeApprovalEntity requireVisibleApproval(
            UUID workspaceId,
            UUID approvalId,
            WorkspaceAuthorizationService.WorkspaceAccess access
    ) {
        CreativeApprovalEntity approval = approvalRepository.findByIdAndWorkspaceIdAndDeletedFalse(approvalId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATIVE_APPROVAL_NOT_FOUND));
        if (!isReviewer(access) && !approval.getSubmittedBy().equals(access.currentUser().userId())) {
            activityLogger.logAuthorizationFailure(workspaceId, access.currentUser().userId(), "approval_owner_or_reviewer_required");
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED);
        }
        return approval;
    }

    private CreativeOutputEntity requireOutput(UUID workspaceId, UUID outputId) {
        CreativeOutputEntity output = outputRepository.findByIdAndWorkspaceIdAndDeletedFalse(outputId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATIVE_OUTPUT_NOT_FOUND));
        if (output.getStatus() != CreativeGenerationStatus.COMPLETED || output.getGeneratedAssetId() == null) {
            throw new BusinessException(ErrorCode.CREATIVE_OUTPUT_NOT_FOUND);
        }
        return output;
    }

    private CreativeGenerationRequestEntity requireGenerationRequest(UUID workspaceId, UUID requestId) {
        return generationRequestRepository.findByIdAndWorkspaceIdAndDeletedFalse(requestId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GENERATION_REQUEST_NOT_FOUND));
    }

    private void requireOutputVisibleToActor(
            CreativeOutputEntity output,
            CreativeGenerationRequestEntity generationRequest,
            WorkspaceAuthorizationService.WorkspaceAccess access
    ) {
        if (isReviewer(access) || generationRequest.getUserId().equals(access.currentUser().userId())) {
            return;
        }
        activityLogger.logAuthorizationFailure(output.getWorkspaceId(), access.currentUser().userId(), "creative_output_owner_or_reviewer_required");
        throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED);
    }

    private void applyTransition(CreativeApprovalEntity approval, UUID actorId, CreativeApprovalStatus requestedStatus) {
        try {
            transitionValidator.requireTransition(approval.getStatus(), requestedStatus);
        } catch (BusinessException exception) {
            activityLogger.logInvalidTransition(
                    approval.getWorkspaceId(),
                    approval.getId(),
                    actorId,
                    approval.getStatus(),
                    requestedStatus);
            throw exception;
        }
    }

    private void recordHistory(
            CreativeApprovalEntity approval,
            CreativeApprovalAction action,
            CreativeApprovalStatus previousStatus,
            CreativeApprovalStatus newStatus,
            UUID actorId,
            String note
    ) {
        historyRepository.save(CreativeApprovalHistoryEntity.record(
                approval.getWorkspaceId(),
                approval.getId(),
                approval.getCreativeOutputId(),
                action,
                previousStatus,
                newStatus,
                actorId,
                note));
    }

    private void emitNotification(CreativeApprovalEntity approval, CreativeApprovalAction action, UUID actorId) {
        notificationPublisher.publish(new CreativeApprovalNotificationEvent(
                approval.getWorkspaceId(),
                approval.getId(),
                approval.getCreativeOutputId(),
                approval.getGenerationRequestId(),
                action,
                approval.getStatus(),
                actorId,
                approval.getSubmittedBy(),
                approval.getReviewedBy(),
                Instant.now()));
    }

    private void validateApprovalNote(UUID workspaceId, UUID actorId, String note) {
        validateOptionalReviewText(workspaceId, actorId, note, "approval note");
    }

    private void validateDueAt(UUID workspaceId, UUID actorId, Instant dueAt) {
        if (dueAt != null && dueAt.isBefore(Instant.now())) {
            activityLogger.logValidationFailure(workspaceId, actorId, "dueAt must be in the future");
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "dueAt must be in the future");
        }
    }

    private void validateComment(UUID workspaceId, UUID actorId, String comment) {
        if (!StringUtils.hasText(comment)) {
            activityLogger.logValidationFailure(workspaceId, actorId, "comment is required");
            throw new BusinessException(ErrorCode.CREATIVE_REVIEW_COMMENT_INVALID, "Comment is required");
        }
        int length = comment.trim().length();
        if (length < MIN_COMMENT_LENGTH || length > MAX_COMMENT_LENGTH) {
            activityLogger.logValidationFailure(workspaceId, actorId, "comment length is invalid");
            throw new BusinessException(ErrorCode.CREATIVE_REVIEW_COMMENT_INVALID, "Comment must be between 2 and 2000 characters");
        }
    }

    private void requireReviewText(UUID workspaceId, UUID actorId, String value, String label) {
        if (!StringUtils.hasText(value)) {
            activityLogger.logValidationFailure(workspaceId, actorId, label + " is required");
            throw new BusinessException(ErrorCode.CREATIVE_APPROVAL_REASON_REQUIRED, label + " is required");
        }
        validateOptionalReviewText(workspaceId, actorId, value, label);
    }

    private void validateOptionalReviewText(UUID workspaceId, UUID actorId, String value, String label) {
        if (value == null) {
            return;
        }
        int length = value.trim().length();
        if (length > MAX_COMMENT_LENGTH) {
            activityLogger.logValidationFailure(workspaceId, actorId, label + " is too long");
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, label + " must be 2000 characters or fewer");
        }
    }
}
