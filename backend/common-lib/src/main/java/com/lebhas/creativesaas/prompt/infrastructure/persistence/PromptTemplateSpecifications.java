package com.lebhas.creativesaas.prompt.infrastructure.persistence;

import com.lebhas.creativesaas.prompt.application.dto.PromptTemplateFilter;
import com.lebhas.creativesaas.prompt.domain.PromptTemplateEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class PromptTemplateSpecifications {

    private PromptTemplateSpecifications() {
    }

    public static Specification<PromptTemplateEntity> forList(PromptTemplateFilter filter) {
        return notDeleted()
                .and(accessibleScope(filter))
                .and(hasCategory(filter))
                .and(hasPlatform(filter))
                .and(hasCampaignObjective(filter))
                .and(hasLanguage(filter))
                .and(hasBusinessType(filter))
                .and(hasStatus(filter))
                .and(hasKeyword(filter));
    }

    private static Specification<PromptTemplateEntity> notDeleted() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isFalse(root.get("deleted"));
    }

    private static Specification<PromptTemplateEntity> accessibleScope(PromptTemplateFilter filter) {
        return (root, query, criteriaBuilder) -> {
            if (Boolean.TRUE.equals(filter.systemDefault())) {
                return criteriaBuilder.isTrue(root.get("isDefault"));
            }
            if (Boolean.FALSE.equals(filter.systemDefault())) {
                return criteriaBuilder.equal(root.get("workspaceId"), filter.workspaceId());
            }
            if (!filter.includeSystemDefaults()) {
                return criteriaBuilder.equal(root.get("workspaceId"), filter.workspaceId());
            }
            return criteriaBuilder.or(
                    criteriaBuilder.equal(root.get("workspaceId"), filter.workspaceId()),
                    criteriaBuilder.isTrue(root.get("isDefault")));
        };
    }

    private static Specification<PromptTemplateEntity> hasPlatform(PromptTemplateFilter filter) {
        return filter.platform() == null ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("platform"), filter.platform());
    }

    private static Specification<PromptTemplateEntity> hasCategory(PromptTemplateFilter filter) {
        if (!StringUtils.hasText(filter.category())) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                criteriaBuilder.lower(root.get("category")),
                filter.category().trim().toLowerCase());
    }

    private static Specification<PromptTemplateEntity> hasCampaignObjective(PromptTemplateFilter filter) {
        return filter.campaignObjective() == null ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("campaignObjective"), filter.campaignObjective());
    }

    private static Specification<PromptTemplateEntity> hasLanguage(PromptTemplateFilter filter) {
        return filter.language() == null ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("language"), filter.language());
    }

    private static Specification<PromptTemplateEntity> hasBusinessType(PromptTemplateFilter filter) {
        if (!StringUtils.hasText(filter.businessType())) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                criteriaBuilder.lower(root.get("businessType")),
                filter.businessType().trim().toLowerCase());
    }

    private static Specification<PromptTemplateEntity> hasStatus(PromptTemplateFilter filter) {
        return filter.status() == null ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), filter.status());
    }

    private static Specification<PromptTemplateEntity> hasKeyword(PromptTemplateFilter filter) {
        if (!StringUtils.hasText(filter.search())) {
            return null;
        }
        String keyword = "%" + filter.search().trim().toLowerCase() + "%";
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), keyword),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("category")), keyword),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), keyword),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("businessType")), keyword));
    }
}
