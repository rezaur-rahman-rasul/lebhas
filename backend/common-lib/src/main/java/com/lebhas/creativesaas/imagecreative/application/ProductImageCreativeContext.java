package com.lebhas.creativesaas.imagecreative.application;

import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.brand.domain.BrandEntity;
import com.lebhas.creativesaas.campaign.domain.ProjectCampaignEntity;
import com.lebhas.creativesaas.imagecreative.application.dto.ProductImageCreativeRequest;
import com.lebhas.creativesaas.product.domain.ProductServiceEntity;

import java.util.UUID;

public record ProductImageCreativeContext(
        UUID generationId,
        String toolCode,
        ProjectCampaignEntity project,
        BrandEntity brand,
        ProductServiceEntity product,
        AssetEntity productAsset,
        AssetEntity logoAsset,
        ProductImageCreativeRequest request,
        CreativeGenerationContext generationContext
) {
}
