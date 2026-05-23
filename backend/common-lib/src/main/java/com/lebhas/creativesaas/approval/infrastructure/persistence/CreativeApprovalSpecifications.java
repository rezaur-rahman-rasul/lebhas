package com.lebhas.creativesaas.approval.infrastructure.persistence;

import com.lebhas.creativesaas.approval.application.dto.CreativeApprovalListCriteria;
import com.lebhas.creativesaas.approval.domain.CreativeApprovalEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@Deprecated(forRemoval = true)
public final class CreativeApprovalSpecifications {

    private CreativeApprovalSpecifications() {
    }

    public static Specification<CreativeApprovalEntity> forList(CreativeApprovalListCriteria criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("workspaceId"), criteria.workspaceId()));
            predicates.add(builder.isFalse(root.get("deleted")));
            if (criteria.creativeOutputId() != null) {
                predicates.add(builder.equal(root.get("creativeOutputId"), criteria.creativeOutputId()));
            }
            if (criteria.generationRequestId() != null) {
                predicates.add(builder.equal(root.get("generationRequestId"), criteria.generationRequestId()));
            }
            if (criteria.submittedBy() != null) {
                predicates.add(builder.equal(root.get("submittedBy"), criteria.submittedBy()));
            }
            if (criteria.reviewedBy() != null) {
                predicates.add(builder.equal(root.get("reviewedBy"), criteria.reviewedBy()));
            }
            if (criteria.status() != null) {
                predicates.add(builder.equal(root.get("status"), criteria.status()));
            }
            if (criteria.priority() != null) {
                predicates.add(builder.equal(root.get("priority"), criteria.priority()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
