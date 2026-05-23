package com.lebhas.creativesaas.approval.validation;

import com.lebhas.creativesaas.approval.domain.ApprovalStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class ApprovalStateMachine {

    private final Map<ApprovalStatus, Set<ApprovalStatus>> allowedTransitions = new EnumMap<>(ApprovalStatus.class);

    public ApprovalStateMachine() {
        allowedTransitions.put(ApprovalStatus.NOT_SUBMITTED, EnumSet.of(ApprovalStatus.SUBMITTED));
        allowedTransitions.put(ApprovalStatus.SUBMITTED, EnumSet.of(
                ApprovalStatus.IN_REVIEW,
                ApprovalStatus.CANCELLED));
        allowedTransitions.put(ApprovalStatus.IN_REVIEW, EnumSet.of(
                ApprovalStatus.APPROVED,
                ApprovalStatus.REJECTED,
                ApprovalStatus.CHANGES_REQUESTED,
                ApprovalStatus.CANCELLED));
        allowedTransitions.put(ApprovalStatus.CHANGES_REQUESTED, EnumSet.of(
                ApprovalStatus.RESUBMITTED,
                ApprovalStatus.CANCELLED));
        allowedTransitions.put(ApprovalStatus.RESUBMITTED, EnumSet.of(
                ApprovalStatus.IN_REVIEW,
                ApprovalStatus.CANCELLED));
        allowedTransitions.put(ApprovalStatus.APPROVED, EnumSet.noneOf(ApprovalStatus.class));
        allowedTransitions.put(ApprovalStatus.REJECTED, EnumSet.noneOf(ApprovalStatus.class));
        allowedTransitions.put(ApprovalStatus.CANCELLED, EnumSet.noneOf(ApprovalStatus.class));
    }

    public boolean canTransition(ApprovalStatus currentStatus, ApprovalStatus requestedStatus) {
        if (currentStatus == null || requestedStatus == null) {
            return false;
        }
        return nextStates(currentStatus).contains(requestedStatus);
    }

    public Set<ApprovalStatus> nextStates(ApprovalStatus currentStatus) {
        if (currentStatus == null) {
            return Set.of();
        }
        return Set.copyOf(allowedTransitions.getOrDefault(currentStatus, Set.of()));
    }

    public boolean isTerminal(ApprovalStatus status) {
        return nextStates(status).isEmpty();
    }
}
