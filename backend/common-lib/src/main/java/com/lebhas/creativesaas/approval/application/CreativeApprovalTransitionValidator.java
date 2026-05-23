package com.lebhas.creativesaas.approval.application;

import com.lebhas.creativesaas.approval.domain.CreativeApprovalStatus;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
@Deprecated(forRemoval = true)
public class CreativeApprovalTransitionValidator {

    private final Map<CreativeApprovalStatus, Set<CreativeApprovalStatus>> allowedTransitions =
            new EnumMap<>(CreativeApprovalStatus.class);

    public CreativeApprovalTransitionValidator() {
        allowedTransitions.put(CreativeApprovalStatus.DRAFT, EnumSet.of(CreativeApprovalStatus.SUBMITTED));
        allowedTransitions.put(CreativeApprovalStatus.SUBMITTED, EnumSet.of(
                CreativeApprovalStatus.IN_REVIEW,
                CreativeApprovalStatus.CANCELLED));
        allowedTransitions.put(CreativeApprovalStatus.IN_REVIEW, EnumSet.of(
                CreativeApprovalStatus.APPROVED,
                CreativeApprovalStatus.REJECTED,
                CreativeApprovalStatus.REGENERATE_REQUESTED,
                CreativeApprovalStatus.CANCELLED));
        allowedTransitions.put(CreativeApprovalStatus.REGENERATE_REQUESTED, EnumSet.of(CreativeApprovalStatus.SUBMITTED));
        allowedTransitions.put(CreativeApprovalStatus.APPROVED, EnumSet.noneOf(CreativeApprovalStatus.class));
        allowedTransitions.put(CreativeApprovalStatus.REJECTED, EnumSet.noneOf(CreativeApprovalStatus.class));
        allowedTransitions.put(CreativeApprovalStatus.CANCELLED, EnumSet.noneOf(CreativeApprovalStatus.class));
    }

    public void requireTransition(CreativeApprovalStatus currentStatus, CreativeApprovalStatus requestedStatus) {
        if (!allowedTransitions.getOrDefault(currentStatus, Set.of()).contains(requestedStatus)) {
            throw new BusinessException(
                    ErrorCode.CREATIVE_APPROVAL_INVALID_TRANSITION,
                    "Transition from " + currentStatus + " to " + requestedStatus + " is not allowed");
        }
    }
}
