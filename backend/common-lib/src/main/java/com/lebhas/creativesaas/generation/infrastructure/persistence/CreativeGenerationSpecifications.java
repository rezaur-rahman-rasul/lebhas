package com.lebhas.creativesaas.generation.infrastructure.persistence;

import com.lebhas.creativesaas.generation.application.dto.CreativeGenerationListCriteria;
import com.lebhas.creativesaas.generation.domain.CreativeGenerationRequestEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class CreativeGenerationSpecifications {

    private CreativeGenerationSpecifications() {
    }

    public static Specification<CreativeGenerationRequestEntity> forList(CreativeGenerationListCriteria criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("workspaceId"), criteria.workspaceId()));
            predicates.add(builder.isFalse(root.get("deleted")));
            if (criteria.userId() != null) {
                predicates.add(builder.equal(root.get("userId"), criteria.userId()));
            }
            if (criteria.status() != null) {
                predicates.add(builder.equal(root.get("status"), criteria.status()));
            }
            if (criteria.creativeType() != null) {
                predicates.add(builder.equal(root.get("creativeType"), criteria.creativeType()));
            }
            if (criteria.platform() != null) {
                predicates.add(builder.equal(root.get("platform"), criteria.platform()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
