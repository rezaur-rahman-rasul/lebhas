package com.lebhas.creativesaas.approval.validation;

import com.lebhas.creativesaas.approval.domain.ApprovalRequest;
import com.lebhas.creativesaas.approval.domain.ApprovalStatus;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import org.springframework.stereotype.Component;

@Component
public class ApprovalTransitionValidator {

    private final ApprovalStateMachine approvalStateMachine;

    public ApprovalTransitionValidator(ApprovalStateMachine approvalStateMachine) {
        this.approvalStateMachine = approvalStateMachine;
    }

    public boolean isValidTransition(ApprovalStatus currentStatus, ApprovalStatus requestedStatus) {
        return approvalStateMachine.canTransition(currentStatus, requestedStatus);
    }

    public boolean isValidTransition(ApprovalRequest approvalRequest, ApprovalStatus requestedStatus) {
        return approvalRequest != null && isValidTransition(approvalRequest.getCurrentStatus(), requestedStatus);
    }

    public boolean isValidTransition(GeneratedVersionEntity generatedVersion, ApprovalStatus requestedStatus) {
        return generatedVersion != null && isValidTransition(toApprovalStatus(generatedVersion), requestedStatus);
    }

    public void requireTransition(ApprovalStatus currentStatus, ApprovalStatus requestedStatus) {
        if (requestedStatus == ApprovalStatus.APPROVED && currentStatus == ApprovalStatus.APPROVED) {
            throw invalidTransition("Generated version is already approved");
        }
        if (requestedStatus == ApprovalStatus.RESUBMITTED && currentStatus != ApprovalStatus.CHANGES_REQUESTED) {
            throw invalidTransition("Generated version cannot be resubmitted until changes are requested");
        }
        if (!isValidTransition(currentStatus, requestedStatus)) {
            throw invalidTransition("Transition from " + currentStatus + " to " + requestedStatus + " is not allowed");
        }
    }

    public void requireTransition(ApprovalRequest approvalRequest, ApprovalStatus requestedStatus) {
        if (approvalRequest == null) {
            throw invalidTransition("Approval request is required");
        }
        requireTransition(approvalRequest.getCurrentStatus(), requestedStatus);
    }

    public void requireTransition(GeneratedVersionEntity generatedVersion, ApprovalStatus requestedStatus) {
        if (generatedVersion == null) {
            throw invalidTransition("Generated version is required");
        }
        requireTransition(toApprovalStatus(generatedVersion), requestedStatus);
    }

    public void requireNotAlreadyApproved(GeneratedVersionEntity generatedVersion) {
        if (toApprovalStatus(generatedVersion) == ApprovalStatus.APPROVED) {
            throw invalidTransition("Generated version is already approved");
        }
    }

    public void requireResubmissionAllowed(GeneratedVersionEntity generatedVersion) {
        if (toApprovalStatus(generatedVersion) != ApprovalStatus.CHANGES_REQUESTED) {
            throw invalidTransition("Generated version cannot be resubmitted until changes are requested");
        }
    }

    public ApprovalStatus toApprovalStatus(GeneratedVersionEntity generatedVersion) {
        if (generatedVersion == null || generatedVersion.getApprovalStatus() == null) {
            return ApprovalStatus.NOT_SUBMITTED;
        }
        return toApprovalStatus(generatedVersion.getApprovalStatus());
    }

    public ApprovalStatus toApprovalStatus(com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus approvalStatus) {
        if (approvalStatus == null) {
            return ApprovalStatus.NOT_SUBMITTED;
        }
        com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus canonical = approvalStatus.canonical();
        return switch (canonical) {
            case NOT_SUBMITTED -> ApprovalStatus.NOT_SUBMITTED;
            case SUBMITTED -> ApprovalStatus.SUBMITTED;
            case IN_REVIEW -> ApprovalStatus.IN_REVIEW;
            case RESUBMITTED -> ApprovalStatus.RESUBMITTED;
            case APPROVED -> ApprovalStatus.APPROVED;
            case REJECTED -> ApprovalStatus.REJECTED;
            case CHANGES_REQUESTED -> ApprovalStatus.CHANGES_REQUESTED;
            case DRAFT -> ApprovalStatus.NOT_SUBMITTED;
            case PENDING -> ApprovalStatus.SUBMITTED;
        };
    }

    private BusinessException invalidTransition(String message) {
        return new BusinessException(ErrorCode.CREATIVE_APPROVAL_INVALID_TRANSITION, message);
    }
}
