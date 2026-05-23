package com.lebhas.creativesaas.product.infrastructure.persistence;

import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;
import com.lebhas.creativesaas.product.domain.ProductServiceEntity;

import java.util.List;
import java.util.UUID;

public interface ProductServiceRepository extends TenantAwareRepository<ProductServiceEntity> {

    List<ProductServiceEntity> findAllByWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(UUID workspaceId);

    List<ProductServiceEntity> findAllByWorkspaceIdAndBrandIdAndDeletedFalseOrderByCreatedAtDesc(UUID workspaceId, UUID brandId);
}
