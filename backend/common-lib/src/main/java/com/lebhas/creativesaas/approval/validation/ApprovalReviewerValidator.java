package com.lebhas.creativesaas.approval.validation;

import com.lebhas.creativesaas.approval.domain.ApprovalRequest;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipEntity;
import com.lebhas.creativesaas.identity.infrastructure.persistence.WorkspaceMembershipRepository;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
public class ApprovalReviewerValidator {

    private final WorkspaceMembershipRepository workspaceMembershipRepository;

    public ApprovalReviewerValidator(WorkspaceMembershipRepository workspaceMembershipRepository) {
        this.workspaceMembershipRepository = workspaceMembershipRepository;
    }

    public WorkspaceMembershipEntity requireReviewerBelongsToWorkspace(UUID workspaceId, UUID reviewerId) {
        return workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndDeletedFalse(workspaceId, reviewerId)
                .filter(WorkspaceMembershipEntity::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_MEMBER_NOT_FOUND, "Reviewer is not an active workspace member"));
    }

    public boolean hasReviewerPermission(WorkspaceMembershipEntity reviewerMembership) {
        if (reviewerMembership == null) {
            return false;
        }
        Role role = reviewerMembership.getRole();
        Set<Permission> permissions = reviewerMembership.getPermissions();
        return role == Role.ADMIN || role == Role.MASTER || permissions.contains(Permission.CREATIVE_SUBMIT);
    }

    public void requireReviewerPermission(WorkspaceMembershipEntity reviewerMembership) {
        if (!hasReviewerPermission(reviewerMembership)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Reviewer is not permitted to review approvals");
        }
    }

    public boolean isAssignedReviewer(ApprovalRequest approvalRequest, UUID reviewerId) {
        return approvalRequest != null
                && reviewerId != null
                && reviewerId.equals(approvalRequest.getAssignedReviewerId());
    }

    public void requireAssignedReviewer(ApprovalRequest approvalRequest, UUID reviewerId) {
        if (!isAssignedReviewer(approvalRequest, reviewerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Reviewer is not assigned to this approval request");
        }
    }
}
