package com.lebhas.creativesaas.imagecreative.application;

import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.brand.domain.BrandEntity;
import com.lebhas.creativesaas.campaign.domain.ProjectCampaignEntity;
import com.lebhas.creativesaas.imagecreative.application.dto.ProductImageCreativeRequest;
import com.lebhas.creativesaas.product.domain.ProductServiceEntity;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record CreativeGenerationContext(
        Map<String, Object> workspace,
        Map<String, Object> brand,
        Map<String, Object> productService,
        Map<String, Object> campaign,
        Map<String, Object> creativeRequest,
        String language,
        String platform,
        String creativeType,
        String tone,
        String quality,
        Map<String, Object> assets,
        List<String> colors,
        String logo,
        String slogan,
        String audience,
        String campaignObjective,
        String sellingPoints,
        String preferredCTA
) {

    public static CreativeGenerationContext from(
            ProjectCampaignEntity project,
            BrandEntity brand,
            ProductServiceEntity product,
            AssetEntity productAsset,
            ProductImageCreativeRequest request
    ) {
        Map<String, Object> workspace = new LinkedHashMap<>();
        workspace.put("workspaceId", firstNonBlank(
                project == null ? null : string(project.getWorkspaceId()),
                brand == null ? null : string(brand.getWorkspaceId()),
                product == null ? null : string(product.getWorkspaceId())));

        Map<String, Object> brandMap = new LinkedHashMap<>();
        if (brand != null) {
            put(brandMap, "id", string(brand.getId()));
            put(brandMap, "name", brand.getName());
            put(brandMap, "businessType", brand.getBusinessType());
            put(brandMap, "industry", brand.getIndustry());
            put(brandMap, "targetAudience", brand.getTargetAudience());
            put(brandMap, "creativeLanguage", brand.getLanguagePreference() == null ? null : brand.getLanguagePreference().name());
            put(brandMap, "slogan", null);
            put(brandMap, "preferredCTA", brand.getPreferredCta());
            put(brandMap, "primaryColor", brand.getPrimaryColor());
            put(brandMap, "secondaryColor", brand.getSecondaryColor());
            put(brandMap, "logo", null);
            put(brandMap, "website", brand.getWebsite());
            put(brandMap, "facebookUrl", brand.getFacebookUrl());
            put(brandMap, "instagramUrl", brand.getInstagramUrl());
            put(brandMap, "linkedinUrl", brand.getLinkedinUrl());
            put(brandMap, "tiktokUrl", brand.getTiktokUrl());
            put(brandMap, "brandVoice", brand.getBrandVoice());
        }

        Map<String, Object> productMap = new LinkedHashMap<>();
        if (product != null) {
            put(productMap, "id", string(product.getId()));
            put(productMap, "name", product.getName());
            put(productMap, "category", product.getCategory());
            put(productMap, "description", product.getDescription());
            put(productMap, "targetAudience", product.getTargetAudience());
            put(productMap, "sellingPoints", product.getSellingPoints());
        }

        Map<String, Object> campaignMap = new LinkedHashMap<>();
        if (project != null) {
            put(campaignMap, "id", string(project.getId()));
            put(campaignMap, "name", project.getName());
            put(campaignMap, "description", project.getDescription());
            put(campaignMap, "targetPlatform", project.getTargetPlatform());
            put(campaignMap, "campaignObjective", project.getCampaignObjective());
            put(campaignMap, "campaignType", project.getCampaignType());
        }

        Map<String, Object> requestMap = new LinkedHashMap<>();
        if (request != null) {
            put(requestMap, "prompt", request.sourcePrompt());
            put(requestMap, "language", request.language() == null ? null : request.language().name());
            put(requestMap, "platform", request.platform() == null ? null : request.platform().name());
            put(requestMap, "creativeType", request.creativeFormat() == null ? null : request.creativeFormat().name());
            put(requestMap, "quality", request.qualityMode() == null ? null : request.qualityMode().name());
            put(requestMap, "stylePreset", request.stylePreset());
            put(requestMap, "backgroundStyle", request.backgroundStyle());
            put(requestMap, "cta", request.includeCta() != null && !request.includeCta() ? null : request.cta());
            put(requestMap, "includeCta", request.includeCta() == null ? null : request.includeCta().toString());
            put(requestMap, "includeTypography", request.includeTypography() == null ? null : request.includeTypography().toString());
        }

        Map<String, Object> assets = new LinkedHashMap<>();
        if (productAsset != null) {
            put(assets, "productImageAssetId", string(productAsset.getId()));
            put(assets, "productImageName", productAsset.getOriginalFileName());
            put(assets, "productImageMimeType", productAsset.getMimeType());
            put(assets, "productImagePreviewUrl", productAsset.getPreviewUrl());
        }

        String language = firstNonBlank(
                request == null || request.language() == null ? null : request.language().name(),
                string(brandMap.get("creativeLanguage")));
        String platform = firstNonBlank(
                request == null || request.platform() == null ? null : request.platform().name(),
                string(campaignMap.get("targetPlatform")));
        String creativeType = request == null || request.creativeFormat() == null ? null : request.creativeFormat().name();
        String quality = request == null || request.qualityMode() == null ? null : request.qualityMode().name();
        String preferredCta = request != null && request.includeCta() != null && !request.includeCta()
                ? null
                : firstNonBlank(
                        request == null ? null : request.cta(),
                        string(brandMap.get("preferredCTA")));
        String audience = firstNonBlank(
                string(campaignMap.get("targetAudience")),
                string(productMap.get("targetAudience")),
                string(brandMap.get("targetAudience")));
        String campaignObjective = firstNonBlank(
                string(campaignMap.get("campaignObjective")),
                string(campaignMap.get("description")),
                string(productMap.get("description")));
        String sellingPoints = firstNonBlank(
                string(productMap.get("sellingPoints")),
                string(productMap.get("description")),
                string(brandMap.get("businessType")));

        return new CreativeGenerationContext(
                Map.copyOf(workspace),
                Map.copyOf(brandMap),
                Map.copyOf(productMap),
                Map.copyOf(campaignMap),
                Map.copyOf(requestMap),
                language,
                platform,
                creativeType,
                string(requestMap.get("stylePreset")),
                quality,
                Map.copyOf(assets),
                colors(brand),
                null,
                null,
                audience,
                campaignObjective,
                sellingPoints,
                preferredCta);
    }

    private static List<String> colors(BrandEntity brand) {
        if (brand == null) {
            return List.of();
        }
        List<String> colors = new java.util.ArrayList<>();
        if (StringUtils.hasText(brand.getPrimaryColor())) {
            colors.add(brand.getPrimaryColor().trim());
        }
        if (StringUtils.hasText(brand.getSecondaryColor())) {
            colors.add(brand.getSecondaryColor().trim());
        }
        return colors.stream()
                .filter(StringUtils::hasText)
                .toList();
    }

    private static void put(Map<String, Object> target, String key, String value) {
        if (StringUtils.hasText(value)) {
            target.put(key, value.trim());
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
