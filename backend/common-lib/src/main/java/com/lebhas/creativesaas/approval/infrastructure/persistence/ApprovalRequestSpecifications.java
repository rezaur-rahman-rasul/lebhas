package com.lebhas.creativesaas.approval.infrastructure.persistence;

import com.lebhas.creativesaas.approval.application.dto.ApprovalRequestListCriteria;
import com.lebhas.creativesaas.approval.domain.ApprovalRequest;
import com.lebhas.creativesaas.approval.domain.ApprovalStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@Deprecated(forRemoval = true)
public final class ApprovalRequestSpecifications {

    private ApprovalRequestSpecifications() {
    }

    public static Specification<ApprovalRequest> forList(ApprovalRequestListCriteria criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("workspaceId"), criteria.workspaceId()));
            predicates.add(builder.isFalse(root.get("deleted")));

            if (criteria.reviewerId() != null) {
                predicates.add(builder.equal(root.get("assignedReviewerId"), criteria.reviewerId()));
            }
            if (criteria.submittedBy() != null) {
                predicates.add(builder.equal(root.get("submittedBy"), criteria.submittedBy()));
            }
            if (criteria.submittedFrom() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("submittedAt"), criteria.submittedFrom()));
            }
            if (criteria.submittedTo() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("submittedAt"), criteria.submittedTo()));
            }

            if (criteria.pendingOnly()) {
                predicates.add(root.get("currentStatus").in(ApprovalStatus.SUBMITTED, ApprovalStatus.RESUBMITTED));
            } else if (criteria.approvedOnly()) {
                predicates.add(builder.equal(root.get("currentStatus"), ApprovalStatus.APPROVED));
            } else if (criteria.status() != null) {
                predicates.add(builder.equal(root.get("currentStatus"), criteria.status()));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
