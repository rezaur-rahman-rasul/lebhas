package com.lebhas.creativesaas.product.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "product_services", schema = "platform")
public class ProductServiceEntity extends TenantAwareEntity {

    @Column(name = "brand_id", nullable = false, updatable = false)
    private UUID brandId;

    @Column(name = "name", nullable = false, length = 140)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "category", length = 120)
    private String category;

    @Column(name = "target_audience", length = 240)
    private String targetAudience;

    @Column(name = "selling_points", columnDefinition = "TEXT")
    private String sellingPoints;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductServiceStatus status;

    protected ProductServiceEntity() {
    }

    public static ProductServiceEntity create(
            UUID workspaceId,
            UUID brandId,
            String name,
            String description,
            String category,
            String targetAudience,
            String sellingPoints
    ) {
        ProductServiceEntity entity = new ProductServiceEntity();
        entity.assignWorkspace(workspaceId);
        entity.brandId = require(brandId, "brandId");
        entity.name = normalizeRequired(name, "name");
        entity.description = normalizeNullable(description);
        entity.category = normalizeNullable(category);
        entity.targetAudience = normalizeNullable(targetAudience);
        entity.sellingPoints = normalizeNullable(sellingPoints);
        entity.status = ProductServiceStatus.ACTIVE;
        return entity;
    }

    public UUID getBrandId() {
        return brandId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getTargetAudience() {
        return targetAudience;
    }

    public String getSellingPoints() {
        return sellingPoints;
    }

    public ProductServiceStatus getStatus() {
        return status;
    }

    public void update(
            String name,
            String description,
            String category,
            String targetAudience,
            String sellingPoints
    ) {
        this.name = normalizeRequired(name, "name");
        this.description = normalizeNullable(description);
        this.category = normalizeNullable(category);
        this.targetAudience = normalizeNullable(targetAudience);
        this.sellingPoints = normalizeNullable(sellingPoints);
    }

    public void changeStatus(ProductServiceStatus status) {
        this.status = status == null ? ProductServiceStatus.ACTIVE : status;
    }

    private static UUID require(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
