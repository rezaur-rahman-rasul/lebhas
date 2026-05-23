package com.lebhas.creativesaas.asset.infrastructure.persistence;

import com.lebhas.creativesaas.asset.application.dto.AssetListCriteria;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.Instant;

public final class AssetSpecifications {

    private AssetSpecifications() {
    }

    public static Specification<AssetEntity> forList(AssetListCriteria criteria) {
        return notDeleted()
                .and(hasWorkspace(criteria.workspaceId()))
                .and(hasProject(criteria.projectId()))
                .and(hasAssetType(criteria))
                .and(hasCategory(criteria))
                .and(hasUploadedBy(criteria))
                .and(hasPreviewStatus(criteria))
                .and(hasProcessingStatus(criteria))
                .and(hasStatus(criteria))
                .and(hasKeyword(criteria))
                .and(createdFrom(criteria))
                .and(createdTo(criteria));
    }

    private static Specification<AssetEntity> notDeleted() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isFalse(root.get("deleted"));
    }

    private static Specification<AssetEntity> hasWorkspace(java.util.UUID workspaceId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("workspaceId"), workspaceId);
    }

    private static Specification<AssetEntity> hasProject(java.util.UUID projectId) {
        return projectId == null ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("projectId"), projectId);
    }

    private static Specification<AssetEntity> hasAssetType(AssetListCriteria criteria) {
        return criteria.assetType() == null ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("assetType"), criteria.assetType());
    }

    private static Specification<AssetEntity> hasCategory(AssetListCriteria criteria) {
        return criteria.assetCategory() == null ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("assetCategory"), criteria.assetCategory());
    }

    private static Specification<AssetEntity> hasUploadedBy(AssetListCriteria criteria) {
        return criteria.uploadedBy() == null ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("uploadedBy"), criteria.uploadedBy());
    }

    private static Specification<AssetEntity> hasPreviewStatus(AssetListCriteria criteria) {
        return criteria.previewStatus() == null ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("previewStatus"), criteria.previewStatus());
    }

    private static Specification<AssetEntity> hasProcessingStatus(AssetListCriteria criteria) {
        return criteria.processingStatus() == null ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("processingStatus"), criteria.processingStatus());
    }

    private static Specification<AssetEntity> hasStatus(AssetListCriteria criteria) {
        return criteria.status() == null ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), criteria.status());
    }

    private static Specification<AssetEntity> hasKeyword(AssetListCriteria criteria) {
        if (!StringUtils.hasText(criteria.keyword())) {
            return null;
        }
        String keyword = "%" + criteria.keyword().trim().toLowerCase() + "%";
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("originalFileName")), keyword),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("displayName")), keyword));
    }

    private static Specification<AssetEntity> createdFrom(AssetListCriteria criteria) {
        Instant createdFrom = criteria.createdFrom();
        return createdFrom == null ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
    }

    private static Specification<AssetEntity> createdTo(AssetListCriteria criteria) {
        Instant createdTo = criteria.createdTo();
        return createdTo == null ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdTo);
    }
}
