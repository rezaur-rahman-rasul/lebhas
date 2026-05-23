package com.lebhas.creativesaas.approval.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "platform.approval.redis")
public class ApprovalRedisCacheProperties {

    private Duration approvalRequestTtl = Duration.ofHours(2);
    private Duration approvalStatusTtl = Duration.ofHours(2);
    private Duration approvalPendingTtl = Duration.ofMinutes(15);
    private Duration approvalReviewerTtl = Duration.ofMinutes(30);
    private Duration approvalWorkflowTtl = Duration.ofHours(2);
    private Duration approvalStateTtl = Duration.ofHours(2);
    private Duration reviewerAssignmentTtl = Duration.ofHours(2);
    private Duration shareLinkTtl = Duration.ofHours(24);
    private Duration approvalLockTtl = Duration.ofSeconds(45);
    private Duration approvalRevisionLockTtl = Duration.ofSeconds(45);

    public Duration getApprovalRequestTtl() {
        return approvalRequestTtl;
    }

    public void setApprovalRequestTtl(Duration approvalRequestTtl) {
        this.approvalRequestTtl = approvalRequestTtl;
    }

    public Duration getApprovalStatusTtl() {
        return approvalStatusTtl;
    }

    public void setApprovalStatusTtl(Duration approvalStatusTtl) {
        this.approvalStatusTtl = approvalStatusTtl;
    }

    public Duration getApprovalPendingTtl() {
        return approvalPendingTtl;
    }

    public void setApprovalPendingTtl(Duration approvalPendingTtl) {
        this.approvalPendingTtl = approvalPendingTtl;
    }

    public Duration getApprovalReviewerTtl() {
        return approvalReviewerTtl;
    }

    public void setApprovalReviewerTtl(Duration approvalReviewerTtl) {
        this.approvalReviewerTtl = approvalReviewerTtl;
    }

    public Duration getApprovalWorkflowTtl() {
        return approvalWorkflowTtl;
    }

    public void setApprovalWorkflowTtl(Duration approvalWorkflowTtl) {
        this.approvalWorkflowTtl = approvalWorkflowTtl;
    }

    public Duration getApprovalStateTtl() {
        return approvalStateTtl;
    }

    public void setApprovalStateTtl(Duration approvalStateTtl) {
        this.approvalStateTtl = approvalStateTtl;
    }

    public Duration getReviewerAssignmentTtl() {
        return reviewerAssignmentTtl;
    }

    public void setReviewerAssignmentTtl(Duration reviewerAssignmentTtl) {
        this.reviewerAssignmentTtl = reviewerAssignmentTtl;
    }

    public Duration getShareLinkTtl() {
        return shareLinkTtl;
    }

    public void setShareLinkTtl(Duration shareLinkTtl) {
        this.shareLinkTtl = shareLinkTtl;
    }

    public Duration getApprovalLockTtl() {
        return approvalLockTtl;
    }

    public void setApprovalLockTtl(Duration approvalLockTtl) {
        this.approvalLockTtl = approvalLockTtl;
    }

    public Duration getApprovalRevisionLockTtl() {
        return approvalRevisionLockTtl;
    }

    public void setApprovalRevisionLockTtl(Duration approvalRevisionLockTtl) {
        this.approvalRevisionLockTtl = approvalRevisionLockTtl;
    }
}
