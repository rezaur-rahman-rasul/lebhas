package com.lebhas.creativesaas.approval.application;

import com.lebhas.creativesaas.approval.application.dto.ApprovalAssignmentView;
import com.lebhas.creativesaas.approval.application.dto.ApprovalAuditLogView;
import com.lebhas.creativesaas.approval.application.dto.ApprovalCommentView;
import com.lebhas.creativesaas.approval.application.dto.ApprovalPendingSummaryView;
import com.lebhas.creativesaas.approval.application.dto.ApprovalRequestListCriteria;
import com.lebhas.creativesaas.approval.application.dto.ApprovalRequestView;
import com.lebhas.creativesaas.approval.application.dto.ApprovalReviewView;
import com.lebhas.creativesaas.approval.application.dto.ApprovalReviewerQueueView;
import com.lebhas.creativesaas.approval.cache.ApprovalCacheService;
import com.lebhas.creativesaas.approval.cache.ApprovalCountCacheService;
import com.lebhas.creativesaas.approval.cache.ApprovalReviewerCacheService;
import com.lebhas.creativesaas.approval.domain.ApprovalRequest;
import com.lebhas.creativesaas.approval.domain.ApprovalStatus;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalAssignmentRepository;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalCommentRepository;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalRequestRepository;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalRequestSpecifications;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalReviewRepository;
import com.lebhas.creativesaas.approval.validation.ApprovalPermissionValidator;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionService;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Deprecated(forRemoval = true)
public class ApprovalQueryService {

    private static final Set<ApprovalStatus> PENDING_STATUSES = Set.of(ApprovalStatus.SUBMITTED, ApprovalStatus.RESUBMITTED);

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalReviewRepository approvalReviewRepository;
    private final ApprovalCommentRepository approvalCommentRepository;
    private final ApprovalAssignmentRepository approvalAssignmentRepository;
    private final ApprovalAuditService approvalAuditService;
    private final GeneratedVersionService generatedVersionService;
    private final ApprovalPermissionValidator approvalPermissionValidator;
    private final ApprovalCacheService approvalCacheService;
    private final ApprovalCountCacheService approvalCountCacheService;
    private final ApprovalReviewerCacheService approvalReviewerCacheService;
    private final ApprovalMapper approvalMapper;

    public ApprovalQueryService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            ApprovalRequestRepository approvalRequestRepository,
            ApprovalReviewRepository approvalReviewRepository,
            ApprovalCommentRepository approvalCommentRepository,
            ApprovalAssignmentRepository approvalAssignmentRepository,
            ApprovalAuditService approvalAuditService,
            GeneratedVersionService generatedVersionService,
            ApprovalPermissionValidator approvalPermissionValidator,
            ApprovalCacheService approvalCacheService,
            ApprovalCountCacheService approvalCountCacheService,
            ApprovalReviewerCacheService approvalReviewerCacheService,
            ApprovalMapper approvalMapper
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.approvalRequestRepository = approvalRequestRepository;
        this.approvalReviewRepository = approvalReviewRepository;
        this.approvalCommentRepository = approvalCommentRepository;
        this.approvalAssignmentRepository = approvalAssignmentRepository;
        this.approvalAuditService = approvalAuditService;
        this.generatedVersionService = generatedVersionService;
        this.approvalPermissionValidator = approvalPermissionValidator;
        this.approvalCacheService = approvalCacheService;
        this.approvalCountCacheService = approvalCountCacheService;
        this.approvalReviewerCacheService = approvalReviewerCacheService;
        this.approvalMapper = approvalMapper;
    }

    @Transactional(readOnly = true)
    public ApprovalRequestView getRequest(UUID workspaceId, UUID approvalRequestId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requireVisibility(workspaceId);
        var cached = approvalCacheService.getApprovalRequest(approvalRequestId).map(approvalMapper::toView);
        if (cached.isPresent()) {
            if (!workspaceId.equals(cached.get().workspaceId()) || !canViewRequest(access, cached.get())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "Approval request is not visible for this user");
            }
            return cached.get();
        }
        ApprovalRequest request = approvalRequestRepository.findByIdAndWorkspaceIdAndDeletedFalse(approvalRequestId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATIVE_APPROVAL_NOT_FOUND, "Approval request not found"));
        requireViewerScope(access, request);
        approvalCacheService.cacheApprovalRequest(approvalMapper.toRequestCacheEntry(request));
        return approvalMapper.toView(request);
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequestView> listRequests(ApprovalRequestListCriteria criteria) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requireVisibility(criteria.workspaceId());
        return approvalRequestRepository.findAll(
                        ApprovalRequestSpecifications.forList(criteria),
                        Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .filter(request -> canViewRequest(access, request))
                .map(approvalMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequestView> listByProjectCampaign(UUID workspaceId, UUID projectCampaignId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requireVisibility(workspaceId);
        return approvalRequestRepository.findAllByWorkspaceIdAndProjectCampaignIdAndDeletedFalseOrderByCreatedAtDesc(
                        workspaceId,
                        projectCampaignId)
                .stream()
                .filter(request -> canViewRequest(access, request))
                .map(approvalMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequestView> listByGeneratedVersion(UUID workspaceId, UUID generatedVersionId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requireVisibility(workspaceId);
        generatedVersionService.requireByIdAndWorkspaceId(workspaceId, generatedVersionId);
        return approvalRequestRepository.findAllByWorkspaceIdAndGeneratedVersionIdAndDeletedFalseOrderByCreatedAtDesc(
                        workspaceId,
                        generatedVersionId)
                .stream()
                .filter(request -> canViewRequest(access, request))
                .map(approvalMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequestView> listAssignedToReviewer(UUID workspaceId, UUID reviewerId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requireVisibility(workspaceId);
        if (!canSeeReviewerQueue(access, workspaceId, reviewerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Reviewer queue is not visible for this user");
        }
        return approvalRequestRepository.findAllByWorkspaceIdAndAssignedReviewerIdAndDeletedFalseOrderByCreatedAtDesc(
                        workspaceId,
                        reviewerId)
                .stream()
                .map(approvalMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApprovalCommentView> listComments(UUID workspaceId, UUID approvalRequestId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requireVisibility(workspaceId);
        ApprovalRequest request = approvalRequestRepository.findByIdAndWorkspaceIdAndDeletedFalse(approvalRequestId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATIVE_APPROVAL_NOT_FOUND, "Approval request not found"));
        requireViewerScope(access, request);
        boolean canSeeInternal = approvalPermissionValidator.canReviewApproval(
                access.currentUser(),
                access.effectiveRole(),
                access.permissions(),
                workspaceId,
                request);
        return approvalCommentRepository.findAllByWorkspaceIdAndApprovalRequestIdAndDeletedFalseOrderByCreatedAtAsc(
                        workspaceId,
                        approvalRequestId)
                .stream()
                .filter(comment -> canSeeInternal || !comment.isInternalOnly())
                .map(approvalMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApprovalReviewView> listReviews(UUID workspaceId, UUID approvalRequestId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requireVisibility(workspaceId);
        ApprovalRequest request = approvalRequestRepository.findByIdAndWorkspaceIdAndDeletedFalse(approvalRequestId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATIVE_APPROVAL_NOT_FOUND, "Approval request not found"));
        requireViewerScope(access, request);
        return approvalReviewRepository.findAllByWorkspaceIdAndApprovalRequestIdAndDeletedFalseOrderByReviewedAtAsc(
                        workspaceId,
                        approvalRequestId)
                .stream()
                .map(approvalMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApprovalAssignmentView> listAssignments(UUID workspaceId, UUID approvalRequestId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requireVisibility(workspaceId);
        ApprovalRequest request = approvalRequestRepository.findByIdAndWorkspaceIdAndDeletedFalse(approvalRequestId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATIVE_APPROVAL_NOT_FOUND, "Approval request not found"));
        requireViewerScope(access, request);
        return approvalAssignmentRepository.findAllByWorkspaceIdAndApprovalRequestIdAndDeletedFalseOrderByAssignedAtDesc(
                        workspaceId,
                        approvalRequestId)
                .stream()
                .map(approvalMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApprovalAuditLogView> listAuditLogs(UUID workspaceId, UUID approvalRequestId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requireVisibility(workspaceId);
        ApprovalRequest request = approvalRequestRepository.findByIdAndWorkspaceIdAndDeletedFalse(approvalRequestId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATIVE_APPROVAL_NOT_FOUND, "Approval request not found"));
        requireViewerScope(access, request);
        return approvalAuditService.listByApprovalRequest(workspaceId, approvalRequestId);
    }

    @Transactional(readOnly = true)
    public ApprovalPendingSummaryView pendingSummary(UUID workspaceId) {
        requireVisibility(workspaceId);
        return approvalCountCacheService.getPendingApprovals(workspaceId)
                .map(approvalMapper::toView)
                .orElseGet(() -> {
                    List<ApprovalRequest> pending = approvalRequestRepository.findAllByWorkspaceIdAndCurrentStatusInAndDeletedFalseOrderByCreatedAtDesc(
                            workspaceId,
                            PENDING_STATUSES);
                    List<ApprovalRequest> inReview = approvalRequestRepository.findAllByWorkspaceIdAndCurrentStatusAndDeletedFalseOrderByCreatedAtDesc(
                            workspaceId,
                            ApprovalStatus.IN_REVIEW);
                    List<ApprovalRequest> changesRequested = approvalRequestRepository.findAllByWorkspaceIdAndCurrentStatusAndDeletedFalseOrderByCreatedAtDesc(
                            workspaceId,
                            ApprovalStatus.CHANGES_REQUESTED);
                    ApprovalPendingSummaryView view = approvalMapper.toView(approvalMapper.toPendingCacheEntry(
                            workspaceId,
                            pending.size(),
                            inReview.size(),
                            changesRequested.size(),
                            pending.stream().map(ApprovalRequest::getId).toList()));
                    approvalCountCacheService.cachePendingApprovals(approvalMapper.toPendingCacheEntry(
                            workspaceId,
                            view.pendingCount(),
                            view.inReviewCount(),
                            view.changesRequestedCount(),
                            view.pendingApprovalRequestIds()));
                    return view;
                });
    }

    @Transactional(readOnly = true)
    public ApprovalReviewerQueueView reviewerQueue(UUID workspaceId, UUID reviewerId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requireVisibility(workspaceId);
        if (!canSeeReviewerQueue(access, workspaceId, reviewerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Reviewer queue is not visible for this user");
        }
        return approvalReviewerCacheService.getReviewerQueue(reviewerId)
                .map(entry -> approvalMapper.toView(entry, workspaceId))
                .orElseGet(() -> {
                    List<ApprovalRequest> requests = approvalRequestRepository.findAllByWorkspaceIdAndAssignedReviewerIdAndDeletedFalseOrderByCreatedAtDesc(
                            workspaceId,
                            reviewerId);
                    int pendingCount = (int) requests.stream()
                            .filter(request -> PENDING_STATUSES.contains(request.getCurrentStatus()))
                            .count();
                    int inReviewCount = (int) requests.stream()
                            .filter(request -> request.getCurrentStatus() == ApprovalStatus.IN_REVIEW)
                            .count();
                    ApprovalReviewerQueueView view = approvalMapper.toReviewerQueueView(
                            reviewerId,
                            workspaceId,
                            pendingCount,
                            inReviewCount,
                            requests.stream().map(ApprovalRequest::getId).toList(),
                            Instant.now());
                    approvalReviewerCacheService.cacheReviewerQueue(approvalMapper.toReviewerCacheEntry(
                            reviewerId,
                            workspaceId,
                            pendingCount,
                            inReviewCount,
                            view.approvalRequestIds()));
                    return view;
                });
    }

    private WorkspaceAuthorizationService.WorkspaceAccess requireVisibility(UUID workspaceId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceContext(workspaceId);
        approvalPermissionValidator.requireApprovalVisibility(
                access.currentUser(),
                access.effectiveRole(),
                access.permissions(),
                workspaceId);
        return access;
    }

    private boolean canViewRequest(WorkspaceAuthorizationService.WorkspaceAccess access, ApprovalRequest request) {
        return approvalPermissionValidator.canReviewApproval(
                access.currentUser(),
                access.effectiveRole(),
                access.permissions(),
                request.getWorkspaceId(),
                request)
                || access.permissions().contains(Permission.GENERATED_VERSION_MANAGE)
                || Objects.equals(request.getSubmittedBy(), access.currentUser().userId());
    }

    private void requireViewerScope(WorkspaceAuthorizationService.WorkspaceAccess access, ApprovalRequest request) {
        if (!canViewRequest(access, request)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Approval request is not visible for this user");
        }
    }

    private boolean canViewRequest(WorkspaceAuthorizationService.WorkspaceAccess access, ApprovalRequestView request) {
        return canReviewGenerally(access, request.workspaceId())
                || Objects.equals(request.assignedReviewerId(), access.currentUser().userId())
                || Objects.equals(request.submittedBy(), access.currentUser().userId());
    }

    private boolean canSeeReviewerQueue(
            WorkspaceAuthorizationService.WorkspaceAccess access,
            UUID workspaceId,
            UUID reviewerId
    ) {
        return canReviewGenerally(access, workspaceId)
                || Objects.equals(access.currentUser().userId(), reviewerId);
    }

    private boolean canReviewGenerally(WorkspaceAuthorizationService.WorkspaceAccess access, UUID workspaceId) {
        if (access.currentUser().isMaster()) {
            return approvalPermissionValidator.hasMasterSupportVisibility(
                    access.currentUser(),
                    access.permissions(),
                    workspaceId);
        }
        return access.effectiveRole() == Role.ADMIN
                || access.permissions().contains(Permission.GENERATED_VERSION_MANAGE);
    }
}
