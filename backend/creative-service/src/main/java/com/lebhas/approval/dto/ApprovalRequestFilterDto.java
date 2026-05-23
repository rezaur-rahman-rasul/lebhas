package com.lebhas.approval.dto;

import com.lebhas.creativesaas.approval.domain.ApprovalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.AssertTrue;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Filters for approval request listings.")
public class ApprovalRequestFilterDto {

    @Schema(description = "Exact approval status filter.")
    private ApprovalStatus status;

    @Schema(description = "Assigned reviewer user id filter.")
    private UUID reviewer;

    @Schema(description = "Submitter user id filter.")
    private UUID submittedBy;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "Inclusive lower bound for submittedAt.", type = "string", format = "date-time")
    private Instant fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "Inclusive upper bound for submittedAt.", type = "string", format = "date-time")
    private Instant toDate;

    @Schema(description = "When true, only SUBMITTED and RESUBMITTED requests are returned.")
    private Boolean pendingOnly = false;

    @Schema(description = "When true, only APPROVED requests are returned.")
    private Boolean approvedOnly = false;

    public ApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(ApprovalStatus status) {
        this.status = status;
    }

    public UUID getReviewer() {
        return reviewer;
    }

    public void setReviewer(UUID reviewer) {
        this.reviewer = reviewer;
    }

    public UUID getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(UUID submittedBy) {
        this.submittedBy = submittedBy;
    }

    public Instant getFromDate() {
        return fromDate;
    }

    public void setFromDate(Instant fromDate) {
        this.fromDate = fromDate;
    }

    public Instant getToDate() {
        return toDate;
    }

    public void setToDate(Instant toDate) {
        this.toDate = toDate;
    }

    public Boolean getPendingOnly() {
        return pendingOnly;
    }

    public void setPendingOnly(Boolean pendingOnly) {
        this.pendingOnly = pendingOnly;
    }

    public Boolean getApprovedOnly() {
        return approvedOnly;
    }

    public void setApprovedOnly(Boolean approvedOnly) {
        this.approvedOnly = approvedOnly;
    }

    @AssertTrue(message = "fromDate must be before or equal to toDate")
    public boolean isDateRangeValid() {
        return fromDate == null || toDate == null || !fromDate.isAfter(toDate);
    }

    @AssertTrue(message = "pendingOnly and approvedOnly cannot both be true")
    public boolean isExclusiveFlagsValid() {
        return !(Boolean.TRUE.equals(pendingOnly) && Boolean.TRUE.equals(approvedOnly));
    }

    @AssertTrue(message = "approvedOnly can only be combined with status APPROVED")
    public boolean isApprovedOnlyCompatibleWithStatus() {
        return !Boolean.TRUE.equals(approvedOnly) || status == null || status == ApprovalStatus.APPROVED;
    }

    @AssertTrue(message = "pendingOnly can only be combined with status SUBMITTED or RESUBMITTED")
    public boolean isPendingOnlyCompatibleWithStatus() {
        return !Boolean.TRUE.equals(pendingOnly)
                || status == null
                || status == ApprovalStatus.SUBMITTED
                || status == ApprovalStatus.RESUBMITTED;
    }
}
