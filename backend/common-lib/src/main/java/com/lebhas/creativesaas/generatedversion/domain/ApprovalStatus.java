package com.lebhas.creativesaas.generatedversion.domain;

public enum ApprovalStatus {
    NOT_SUBMITTED,
    SUBMITTED,
    IN_REVIEW,
    RESUBMITTED,
    APPROVED,
    REJECTED,
    CHANGES_REQUESTED,
    @Deprecated
    DRAFT,
    @Deprecated
    PENDING;

    public ApprovalStatus canonical() {
        return switch (this) {
            case DRAFT -> NOT_SUBMITTED;
            case PENDING -> SUBMITTED;
            default -> this;
        };
    }
}
