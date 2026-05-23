package com.lebhas.creativesaas.product.application.dto;

import com.lebhas.creativesaas.product.domain.ProductServiceStatus;

import java.time.Instant;
import java.util.UUID;

public record ProductServiceView(
        UUID id,
        UUID workspaceId,
        UUID brandId,
        String name,
        String description,
        String category,
        String targetAudience,
        String sellingPoints,
        ProductServiceStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
