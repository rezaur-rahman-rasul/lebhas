package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.approval.domain.CreativeApprovalPriority;
import com.lebhas.creativesaas.approval.domain.CreativeApprovalStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

public class CreativeApprovalListRequest {

    private UUID creativeOutputId;
    private UUID generationRequestId;
    private UUID submittedBy;
    private UUID reviewedBy;
    private CreativeApprovalStatus status;
    private CreativeApprovalPriority priority;

    @Min(value = 0, message = "Page index must be zero or greater")
    private Integer page = 0;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size must not exceed 100")
    private Integer size = 20;

    public UUID getCreativeOutputId() {
        return creativeOutputId;
    }

    public void setCreativeOutputId(UUID creativeOutputId) {
        this.creativeOutputId = creativeOutputId;
    }

    public UUID getGenerationRequestId() {
        return generationRequestId;
    }

    public void setGenerationRequestId(UUID generationRequestId) {
        this.generationRequestId = generationRequestId;
    }

    public UUID getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(UUID submittedBy) {
        this.submittedBy = submittedBy;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(UUID reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public CreativeApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(CreativeApprovalStatus status) {
        this.status = status;
    }

    public CreativeApprovalPriority getPriority() {
        return priority;
    }

    public void setPriority(CreativeApprovalPriority priority) {
        this.priority = priority;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
