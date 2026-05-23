package com.lebhas.creativesaas.approval.application;

import com.lebhas.approval.event.ApprovalLifecycleEvent;
import com.lebhas.approval.producer.ApprovalEventProducer;
import com.lebhas.creativesaas.approval.application.dto.AddApprovalCommentCommand;
import com.lebhas.creativesaas.approval.application.dto.ApprovalCommentView;
import com.lebhas.creativesaas.approval.cache.ApprovalCacheService;
import com.lebhas.creativesaas.approval.domain.ApprovalAuditAction;
import com.lebhas.creativesaas.approval.domain.ApprovalComment;
import com.lebhas.creativesaas.approval.domain.ApprovalRequest;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalCommentRepository;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalRequestRepository;
import com.lebhas.creativesaas.approval.validation.ApprovalPermissionValidator;
import com.lebhas.creativesaas.approval.validation.ApprovalWorkspaceValidator;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionService;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Service
@Deprecated(forRemoval = true)
public class ApprovalCommentService {

    private static final int MAX_COMMENT_LENGTH = 2000;

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalCommentRepository approvalCommentRepository;
    private final GeneratedVersionService generatedVersionService;
    private final ApprovalWorkspaceValidator approvalWorkspaceValidator;
    private final ApprovalPermissionValidator approvalPermissionValidator;
    private final ApprovalAuditService approvalAuditService;
    private final ApprovalCacheService approvalCacheService;
    private final ApprovalMapper approvalMapper;
    private final ApprovalEventProducer approvalEventProducer;

    public ApprovalCommentService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            ApprovalRequestRepository approvalRequestRepository,
            ApprovalCommentRepository approvalCommentRepository,
            GeneratedVersionService generatedVersionService,
            ApprovalWorkspaceValidator approvalWorkspaceValidator,
            ApprovalPermissionValidator approvalPermissionValidator,
            ApprovalAuditService approvalAuditService,
            ApprovalCacheService approvalCacheService,
            ApprovalMapper approvalMapper,
            ApprovalEventProducer approvalEventProducer
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.approvalRequestRepository = approvalRequestRepository;
        this.approvalCommentRepository = approvalCommentRepository;
        this.generatedVersionService = generatedVersionService;
        this.approvalWorkspaceValidator = approvalWorkspaceValidator;
        this.approvalPermissionValidator = approvalPermissionValidator;
        this.approvalAuditService = approvalAuditService;
        this.approvalCacheService = approvalCacheService;
        this.approvalMapper = approvalMapper;
        this.approvalEventProducer = approvalEventProducer;
    }

    @Transactional
    public ApprovalCommentView addComment(AddApprovalCommentCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceContext(command.workspaceId());
        ApprovalRequest request = approvalWorkspaceValidator.requireApprovalRequestBelongsToWorkspace(
                command.workspaceId(),
                command.approvalRequestId());
        GeneratedVersionEntity version = approvalWorkspaceValidator.requireGeneratedVersionBelongsToWorkspace(
                command.workspaceId(),
                request.getGeneratedVersionId());
        approvalWorkspaceValidator.requireApprovalRequestMatchesGeneratedVersion(request, version);

        boolean reviewerActor = approvalPermissionValidator.canReviewApproval(
                access.currentUser(),
                access.effectiveRole(),
                access.permissions(),
                command.workspaceId(),
                request);
        boolean submitterActor = Objects.equals(request.getSubmittedBy(), access.currentUser().userId())
                && access.permissions().contains(Permission.CREATIVE_SUBMIT);
        if (!reviewerActor && !submitterActor) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Commenting on this approval request is not permitted for this user");
        }
        if (command.internalOnly() && !reviewerActor) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Internal approval comments are only allowed for reviewers");
        }
        validateComment(command.commentText());

        ApprovalComment comment = approvalCommentRepository.save(ApprovalComment.create(
                command.workspaceId(),
                request.getId(),
                version.getId(),
                access.currentUser().userId(),
                command.commentText(),
                command.internalOnly()));

        request.updateLatestComment(command.commentText());
        approvalRequestRepository.save(request);

        version.recordApprovalComment(reviewerActor ? access.currentUser().userId() : null, command.commentText());
        version = generatedVersionService.save(version);

        ApprovalLifecycleEvent event = ApprovalLifecycleEvent.from(
                request,
                access.currentUser().userId(),
                request.getCurrentStatus(),
                request.getCurrentStatus(),
                command.commentText(),
                command.internalOnly(),
                null);

        approvalAuditService.record(
                event.eventId(),
                command.workspaceId(),
                request.getId(),
                version.getId(),
                access.currentUser().userId(),
                ApprovalAuditAction.COMMENT_CREATED,
                request.getCurrentStatus(),
                request.getCurrentStatus(),
                command.commentText());

        approvalCacheService.cacheApprovalRequest(approvalMapper.toRequestCacheEntry(request));
        approvalCacheService.cacheApprovalStatus(approvalMapper.toStatusCacheEntry(request, version));
        approvalEventProducer.publishCommentCreated(event);
        return approvalMapper.toView(comment);
    }

    private void validateComment(String commentText) {
        if (!StringUtils.hasText(commentText)) {
            throw new BusinessException(ErrorCode.CREATIVE_REVIEW_COMMENT_INVALID, "Comment text is required");
        }
        String normalized = commentText.trim();
        if (normalized.length() > MAX_COMMENT_LENGTH) {
            throw new BusinessException(ErrorCode.CREATIVE_REVIEW_COMMENT_INVALID, "Comment text must be 2000 characters or fewer");
        }
    }
}
