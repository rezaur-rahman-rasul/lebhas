package com.lebhas.creativesaas.texttool.application;

import com.lebhas.creativesaas.brand.domain.BrandEntity;
import com.lebhas.creativesaas.product.domain.ProductServiceEntity;
import com.lebhas.creativesaas.project.domain.ProjectEntity;
import com.lebhas.creativesaas.texttool.application.dto.CreativeTextToolRequest;
import com.lebhas.creativesaas.texttool.domain.CreativeTextToolType;

public record TextToolGenerationContext(
        CreativeTextToolType toolType,
        String toolCode,
        ProjectEntity project,
        BrandEntity brand,
        ProductServiceEntity productService,
        CreativeTextToolRequest request
) {
}
