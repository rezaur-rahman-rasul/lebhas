package com.lebhas.creativesaas.prompt.infrastructure.persistence;

import com.lebhas.creativesaas.prompt.application.dto.PromptHistoryFilter;
import com.lebhas.creativesaas.prompt.domain.PromptHistoryEntity;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class PromptHistorySpecifications {

    private PromptHistorySpecifications() {
    }

    public static Specification<PromptHistoryEntity> forList(PromptHistoryFilter filter) {
        return andAll(
                notDeleted(),
                hasWorkspace(filter.workspaceId()),
                hasProject(filter),
                hasUser(filter),
                hasSuggestionType(filter),
                hasPlatform(filter),
                hasCampaignObjective(filter),
                hasStatus(filter),
                createdFrom(filter),
                createdTo(filter));
    }

    @SafeVarargs
    private static Specification<PromptHistoryEntity> andAll(Specification<PromptHistoryEntity>... specifications) {
        Specification<PromptHistoryEntity> combined = null;
        for (Specification<PromptHistoryEntity> specification : specifications) {
            if (specification == null) {
                continue;
            }
            combined = combined == null ? specification : combined.and(specification);
        }
        return combined;
    }

    private static Specification<PromptHistoryEntity> notDeleted() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isFalse(root.get("deleted"));
    }

    private static Specification<PromptHistoryEntity> hasWorkspace(java.util.UUID workspaceId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("workspaceId"), workspaceId);
    }

    private static Specification<PromptHistoryEntity> hasUser(PromptHistoryFilter filter) {
        return filter.userId() == null ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("userId"), filter.userId());
    }

    private static Specification<PromptHistoryEntity> hasProject(PromptHistoryFilter filter) {
        if (filter.projectId() == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.equal(root.get("projectCampaignId"), filter.projectId()),
                criteriaBuilder.and(
                        criteriaBuilder.isNull(root.get("projectCampaignId")),
                        criteriaBuilder.equal(root.get("projectId"), filter.projectId())));
    }

    private static Specification<PromptHistoryEntity> hasSuggestionType(PromptHistoryFilter filter) {
        return filter.suggestionType() == null ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("suggestionType"), filter.suggestionType());
    }

    private static Specification<PromptHistoryEntity> hasPlatform(PromptHistoryFilter filter) {
        return filter.platform() == null ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("platform"), filter.platform());
    }

    private static Specification<PromptHistoryEntity> hasCampaignObjective(PromptHistoryFilter filter) {
        return filter.campaignObjective() == null ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("campaignObjective"), filter.campaignObjective());
    }

    private static Specification<PromptHistoryEntity> hasStatus(PromptHistoryFilter filter) {
        return filter.status() == null ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), filter.status());
    }

    private static Specification<PromptHistoryEntity> createdFrom(PromptHistoryFilter filter) {
        Instant createdFrom = filter.createdFrom();
        return createdFrom == null ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
    }

    private static Specification<PromptHistoryEntity> createdTo(PromptHistoryFilter filter) {
        Instant createdTo = filter.createdTo();
        return createdTo == null ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdTo);
    }
}
