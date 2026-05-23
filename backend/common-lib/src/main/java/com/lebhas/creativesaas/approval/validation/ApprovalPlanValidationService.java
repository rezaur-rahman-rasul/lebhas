package com.lebhas.creativesaas.approval.validation;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.pricing.application.WorkspacePlanContextService;
import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspacePlanContextView;
import com.lebhas.pricing.WorkspaceSubscriptionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class ApprovalPlanValidationService {

    private final WorkspacePlanContextService workspacePlanContextService;

    public ApprovalPlanValidationService(WorkspacePlanContextService workspacePlanContextService) {
        this.workspacePlanContextService = workspacePlanContextService;
    }

    @Transactional(readOnly = true)
    public void requireApprovalWorkflowEnabled(UUID workspaceId) {
        if (!isApprovalWorkflowEnabled(workspaceId)) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "Approval workflow is not enabled for the workspace plan");
        }
    }

    @Transactional(readOnly = true)
    public boolean isApprovalWorkflowEnabled(UUID workspaceId) {
        return findActivePlanFeaturePolicy(workspaceId)
                .map(PlanFeaturePolicyView::allowApprovalWorkflow)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Optional<PlanFeaturePolicyView> findActivePlanFeaturePolicy(UUID workspaceId) {
        if (workspaceId == null) {
            return Optional.empty();
        }
        WorkspacePlanContextView context = workspacePlanContextService.getWorkspacePlanContext(workspaceId);
        if (context.subscription() == null
                || !isUsableSubscriptionStatus(context.subscription().status())
                || context.pricingPlan() == null
                || !context.pricingPlan().active()) {
            return Optional.empty();
        }
        return Optional.ofNullable(context.featurePolicy());
    }

    private boolean isUsableSubscriptionStatus(WorkspaceSubscriptionStatus status) {
        return status == WorkspaceSubscriptionStatus.ACTIVE || status == WorkspaceSubscriptionStatus.TRIAL;
    }
}
