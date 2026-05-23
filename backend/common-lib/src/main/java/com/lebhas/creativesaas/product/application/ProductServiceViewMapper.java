package com.lebhas.creativesaas.product.application;

import com.lebhas.creativesaas.product.application.dto.ProductServiceView;
import com.lebhas.creativesaas.product.domain.ProductServiceEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductServiceViewMapper {

    public ProductServiceView toView(ProductServiceEntity entity) {
        return new ProductServiceView(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getBrandId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getTargetAudience(),
                entity.getSellingPoints(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
