package com.lebhas.creativesaas.approval.cache;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
public class ApprovalRedisTtlStrategy {

    private final ApprovalRedisCacheProperties properties;
    private final Clock clock;

    public ApprovalRedisTtlStrategy(ApprovalRedisCacheProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public Duration approvalRequestTtl() {
        return normalize(properties.getApprovalRequestTtl(), Duration.ofHours(2));
    }

    public Duration approvalStatusTtl() {
        return normalize(properties.getApprovalStatusTtl(), Duration.ofHours(2));
    }

    public Duration approvalPendingTtl() {
        return normalize(properties.getApprovalPendingTtl(), Duration.ofMinutes(15));
    }

    public Duration approvalReviewerTtl() {
        return normalize(properties.getApprovalReviewerTtl(), Duration.ofMinutes(30));
    }

    public Duration approvalWorkflowTtl() {
        return normalize(properties.getApprovalWorkflowTtl(), Duration.ofHours(2));
    }

    public Duration approvalStateTtl() {
        return normalize(properties.getApprovalStateTtl(), Duration.ofHours(2));
    }

    public Duration reviewerAssignmentTtl() {
        return normalize(properties.getReviewerAssignmentTtl(), Duration.ofHours(2));
    }

    public Duration shareLinkTtl(Instant expiresAt) {
        Duration configured = normalize(properties.getShareLinkTtl(), Duration.ofHours(24));
        if (expiresAt == null) {
            return configured;
        }
        Duration untilExpiry = Duration.between(clock.instant(), expiresAt);
        if (untilExpiry.isNegative() || untilExpiry.isZero()) {
            return Duration.ofSeconds(1);
        }
        return untilExpiry.compareTo(configured) < 0 ? untilExpiry : configured;
    }

    public Duration approvalLockTtl() {
        return normalize(properties.getApprovalLockTtl(), Duration.ofSeconds(45));
    }

    public Duration approvalRevisionLockTtl() {
        return normalize(properties.getApprovalRevisionLockTtl(), Duration.ofSeconds(45));
    }

    private Duration normalize(Duration candidate, Duration fallback) {
        if (candidate == null || candidate.isNegative() || candidate.isZero()) {
            return fallback;
        }
        return candidate;
    }
}
